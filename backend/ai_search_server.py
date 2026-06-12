import json
import os
import re
import socket
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from fastapi import FastAPI
from pydantic import BaseModel, Field


PROJECT_ROOT = Path(__file__).resolve().parents[1]
ADS_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "mock_ads.json"

QWEN_BASE_URL = os.getenv("QWEN_BASE_URL", "http://100.65.29.50:11434/v1").rstrip("/")
QWEN_MODEL = os.getenv("QWEN_MODEL", "qwen3:8b")
QWEN_API_KEY = os.getenv("QWEN_API_KEY", "EMPTY")

VALID_CHANNELS = {"FEATURED", "ECOMMERCE", "LOCAL"}
VALID_INTENTS = {"BROWSE", "PURCHASE", "LOCAL_DEAL", "SCENE", "GIFT", "COMPARE"}
PURCHASE_HINTS = ["买", "购买", "手机", "耳机", "充电", "数码", "下单", "装备"]
LOCAL_HINTS = ["附近", "周末", "门店", "优惠", "咖啡", "轻食", "奶茶"]
SCENE_BROWSE_HINTS = ["旅行", "露营", "推荐", "放松"]
CUP_PRODUCT_HINTS = ["杯子", "水杯", "咖啡杯", "保温杯", "马克杯", "餐具", "厨房用品"]
CUP_PRODUCT_KEYWORDS = ["杯子", "水杯", "咖啡杯", "厨房用品", "生活用品", "家居用品"]
CUP_DIRECT_TERMS = ["咖啡杯", "杯子", "水杯", "保温杯", "马克杯", "厨房用品", "餐具"]
CUP_RELATED_TERMS = ["厨房", "家居", "生活用品", "家居用品", "居家", "收纳"]
DIGITAL_DIRECT_TERMS = ["手机", "耳机", "充电器", "移动电源", "充电", "数码"]
DIGITAL_RELATED_TERMS = ["数码", "桌面", "通勤", "办公", "学生优选"]
CUP_PRODUCT_REFINEMENTS = ["咖啡杯", "水杯", "厨房用品", "生活用品", "家居好物"]


class AiSearchRequest(BaseModel):
    query: str = Field(default="")
    currentChannel: Optional[str] = None
    limit: int = Field(default=12, ge=1, le=50)


@dataclass
class ToolCall:
    name: str
    arguments: Dict[str, Any]
    source: str
    fallback_reason: str = ""
    qwen_tool_name: str = ""
    qwen_raw_content_preview: str = ""


class QwenCallError(Exception):
    def __init__(
        self,
        reason: str,
        message: str,
        qwen_tool_name: str = "",
        qwen_raw_content_preview: str = "",
    ):
        super().__init__(message)
        self.reason = reason
        self.qwen_tool_name = qwen_tool_name
        self.qwen_raw_content_preview = qwen_raw_content_preview


app = FastAPI(title="AI Ad Search Backend")


def load_ads() -> List[Dict[str, Any]]:
    with ADS_PATH.open("r", encoding="utf-8") as file:
        data = json.load(file)
    if not isinstance(data, list):
        return []
    return [ad for ad in data if isinstance(ad, dict) and str(ad.get("id", "")).strip()]


ADS = load_ads()
ADS_BY_ID = {str(ad.get("id")): ad for ad in ADS}


SEARCH_ADS_TOOL = {
    "type": "function",
    "function": {
        "name": "search_ads",
        "description": "根据用户自然语言需求搜索本地广告池",
        "parameters": {
            "type": "object",
            "properties": {
                "intent": {
                    "type": "string",
                    "enum": ["BROWSE", "PURCHASE", "LOCAL_DEAL", "SCENE", "GIFT", "COMPARE"],
                },
                "preferredChannel": {
                    "type": ["string", "null"],
                    "enum": ["FEATURED", "ECOMMERCE", "LOCAL", None],
                },
                "tags": {"type": "array", "items": {"type": "string"}},
                "categories": {"type": "array", "items": {"type": "string"}},
                "scenes": {"type": "array", "items": {"type": "string"}},
                "audiences": {"type": "array", "items": {"type": "string"}},
                "keywords": {"type": "array", "items": {"type": "string"}},
                "excludeTags": {"type": "array", "items": {"type": "string"}},
                "explanation": {"type": "string"},
                "suggestedRefinements": {"type": "array", "items": {"type": "string"}},
            },
            "required": ["intent", "keywords", "explanation"],
        },
    },
}

CLARIFY_SEARCH_TOOL = {
    "type": "function",
    "function": {
        "name": "clarify_search",
        "description": "当用户需求过于模糊时，提出一个简短追问",
        "parameters": {
            "type": "object",
            "properties": {
                "question": {"type": "string"},
                "suggestedOptions": {"type": "array", "items": {"type": "string"}},
            },
            "required": ["question"],
        },
    },
}


def normalize_text(value: Any) -> str:
    return str(value or "").strip().lower()


def normalize_list(value: Any) -> List[str]:
    if value is None:
        return []
    if isinstance(value, list):
        raw_items = value
    else:
        raw_items = re.split(r"[\s,，、;；/]+", str(value))
    result: List[str] = []
    seen = set()
    for item in raw_items:
        text = str(item or "").strip()
        key = text.lower()
        if text and key not in seen:
            seen.add(key)
            result.append(text)
    return result


def sanitize_search_args(args: Dict[str, Any], query: str, current_channel: Optional[str]) -> Dict[str, Any]:
    preferred_channel = args.get("preferredChannel")
    intent = str(args.get("intent", "BROWSE")).upper()
    if intent not in VALID_INTENTS:
        intent = "BROWSE"
    if is_cup_product_query(query):
        intent = "PURCHASE"
    elif has_query_hint(query, LOCAL_HINTS):
        intent = "LOCAL_DEAL"

    keywords = normalize_list(args.get("keywords"))
    if not keywords:
        keywords = split_query_keywords(query)
    if is_cup_product_query(query):
        keywords = CUP_PRODUCT_KEYWORDS

    preferred_channel = choose_preferred_channel(
        query=query,
        intent=intent,
        preferred_channel=preferred_channel,
        current_channel=current_channel,
    )

    tags = normalize_list(args.get("tags"))
    categories = normalize_list(args.get("categories"))
    suggested_refinements = normalize_list(args.get("suggestedRefinements"))[:6]
    if is_cup_product_query(query):
        tags = ["杯子", "水杯", "咖啡杯", "生活用品", "家居用品"]
        categories = ["厨房用品", "生活用品", "家居用品"]
        suggested_refinements = CUP_PRODUCT_REFINEMENTS

    return {
        "intent": intent,
        "preferredChannel": preferred_channel,
        "tags": tags,
        "categories": categories,
        "scenes": normalize_list(args.get("scenes")),
        "audiences": normalize_list(args.get("audiences")),
        "keywords": keywords,
        "excludeTags": normalize_list(args.get("excludeTags")),
        "explanation": build_explanation(
            query=query,
            channel=preferred_channel,
            keywords=keywords,
            model_explanation=str(args.get("explanation") or ""),
        ),
        "suggestedRefinements": suggested_refinements,
    }


def split_query_keywords(query: str) -> List[str]:
    parts = re.split(r"[\s,，。.!！?？、；;：:]+", query.strip())
    keywords = [part.strip() for part in parts if len(part.strip()) >= 2]
    return keywords or ([query.strip()] if query.strip() else [])


def build_default_explanation(query: str, channel: Optional[str], keywords: List[str]) -> str:
    channel_text = {
        "FEATURED": "精选",
        "ECOMMERCE": "电商",
        "LOCAL": "本地",
        None: "全频道",
    }.get(channel, "全频道")
    keyword_text = "、".join(keywords[:4]) if keywords else query
    return f"已按「{keyword_text}」在{channel_text}广告池中筛选真实广告，优先展示相关度较高的内容。"


def has_query_hint(query: str, hints: List[str]) -> bool:
    normalized_query = query.strip().lower()
    return any(hint.lower() in normalized_query for hint in hints)


def is_cup_product_query(query: str) -> bool:
    normalized_query = query.strip().lower()
    if any(hint.lower() in normalized_query for hint in CUP_PRODUCT_HINTS):
        return True
    if "杯" not in normalized_query:
        return False
    if re.search(r"(喝|来|点|要|想喝|想来)\s*(一|1)?\s*杯", normalized_query):
        return False
    return has_query_hint(normalized_query, ["买", "购买", "换", "换个", "换一个", "下单"])


def choose_preferred_channel(
    query: str,
    intent: str,
    preferred_channel: Any,
    current_channel: Optional[str],
) -> Optional[str]:
    channel = preferred_channel if preferred_channel in VALID_CHANNELS else None

    if is_cup_product_query(query):
        return "ECOMMERCE"
    if has_query_hint(query, LOCAL_HINTS):
        return "LOCAL"
    if has_query_hint(query, PURCHASE_HINTS):
        return "ECOMMERCE"
    if has_query_hint(query, SCENE_BROWSE_HINTS):
        return "FEATURED"

    if channel is not None:
        return channel
    if intent == "LOCAL_DEAL":
        return "LOCAL"
    if intent in {"PURCHASE", "COMPARE", "GIFT"}:
        return "ECOMMERCE"
    if intent in {"SCENE", "BROWSE"}:
        return current_channel if current_channel in VALID_CHANNELS else "FEATURED"
    return current_channel if current_channel in VALID_CHANNELS else None


def has_direct_phone_ad() -> bool:
    phone_pattern = re.compile(r"手机|智能手机|phone", flags=re.IGNORECASE)
    return any(
        phone_pattern.search(
            ad_text(ad, "title", "brand", "brandName", "summary", "description", "category", "tags", "keywords")
        )
        for ad in ADS
    )


def has_direct_term_ad(term: str) -> bool:
    return any(
        term.lower()
        in ad_text(ad, "title", "brand", "brandName", "summary", "description", "category", "tags", "keywords")
        for ad in ADS
    )


def explicit_product_profile(query: str) -> Optional[Dict[str, Any]]:
    if is_cup_product_query(query):
        return {
            "directTerms": CUP_DIRECT_TERMS,
            "relatedTerms": CUP_RELATED_TERMS,
            "directExplanation": "已按「咖啡杯、杯子、水杯、厨房用品」筛选，优先展示直接命中商品词的真实广告。",
            "relatedExplanation": "已识别为「咖啡杯 / 水杯 / 厨房用品」相关需求。当前广告池暂无直接咖啡杯广告，以下展示生活家居类相关内容。",
            "fallbackExplanation": "已识别为「咖啡杯 / 水杯 / 厨房用品」相关需求。当前仅找到弱相关内容，建议尝试「水杯」或「厨房用品」。",
        }
    if has_query_hint(query, ["手机", "耳机", "充电", "数码"]):
        return {
            "directTerms": DIGITAL_DIRECT_TERMS,
            "relatedTerms": DIGITAL_RELATED_TERMS,
            "directExplanation": "已按「手机、耳机、充电器、数码配件」筛选，优先展示直接命中商品词的真实广告。",
            "relatedExplanation": "已识别为数码购买需求。当前广告池暂无直接手机广告，以下展示数码配件、通勤和桌面设备相关内容。",
            "fallbackExplanation": "已识别为数码购买需求。当前仅找到弱相关内容，建议尝试「耳机」或「充电器」。",
        }
    return None


def build_explanation(
    query: str,
    channel: Optional[str],
    keywords: List[str],
    model_explanation: str,
    match_level: str = "",
) -> str:
    product_profile = explicit_product_profile(query)
    if product_profile is not None:
        if "咖啡杯" in query and not has_direct_term_ad("咖啡杯"):
            if match_level == "fallback":
                return "已识别为「咖啡杯 / 水杯 / 厨房用品」相关需求。当前广告池暂无直接咖啡杯广告，仅找到少量弱相关生活家居内容。"
            return "已识别为「咖啡杯 / 水杯 / 厨房用品」相关需求。当前广告池暂无直接咖啡杯广告，以下展示水杯、厨房用品和生活家居类相关内容。"
        if match_level == "direct":
            return str(product_profile["directExplanation"])
        if match_level == "related":
            return str(product_profile["relatedExplanation"])
        if match_level == "fallback":
            return str(product_profile["fallbackExplanation"])
    if has_query_hint(query, ["手机"]) and not has_direct_phone_ad():
        return "已识别为数码购买需求。当前广告池暂无直接手机广告，以下展示手机相关数码产品、配件和性价比电商内容。"
    if has_query_hint(query, PURCHASE_HINTS + LOCAL_HINTS + SCENE_BROWSE_HINTS):
        return build_default_explanation(query, channel, keywords)
    return model_explanation.strip() or build_default_explanation(query, channel, keywords)


def build_suggested_refinements(args: Dict[str, Any]) -> List[str]:
    refinements = normalize_list(args.get("suggestedRefinements"))
    if refinements:
        return refinements[:6]

    candidates = []
    candidates.extend(args.get("tags", [])[:2])
    candidates.extend(args.get("categories", [])[:2])
    candidates.extend(args.get("audiences", [])[:1])
    candidates.extend(args.get("scenes", [])[:1])
    return normalize_list(candidates)[:6]


def call_qwen(query: str, current_channel: Optional[str], limit: int) -> ToolCall:
    payload = {
        "model": QWEN_MODEL,
        "messages": [
            {
                "role": "system",
                "content": (
                    "你是广告搜索参数解析器。你不能编造广告 id，也不能直接生成广告列表。"
                    "请在 search_ads 和 clarify_search 两个工具中选择一个。"
                    "currentChannel 只是用户当前所在 Tab 的上下文，不是强约束。"
                    "用户自然语言意图必须优先于 currentChannel：购买/下单/数码配件优先 ECOMMERCE；"
                    "附近/门店/本地优惠/咖啡轻食优先 LOCAL；旅行/露营/放松/泛推荐场景可优先 FEATURED。"
                    "当用户需求过于宽泛且无法判断方向时使用 clarify_search；否则使用 search_ads，"
                    "把自然语言需求转成 intent、preferredChannel、tags、categories、scenes、audiences、keywords、excludeTags。"
                    "解释要简短、中文、面向用户；不要暗示广告池中存在未确认的具体商品。"
                ),
            },
            {
                "role": "user",
                "content": json.dumps(
                    {
                        "query": query,
                        "currentChannel": current_channel,
                        "limit": limit,
                    },
                    ensure_ascii=False,
                ),
            },
        ],
        "tools": [SEARCH_ADS_TOOL, CLARIFY_SEARCH_TOOL],
        "tool_choice": "auto",
        "temperature": 0.1,
    }
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        f"{QWEN_BASE_URL}/chat/completions",
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {QWEN_API_KEY}",
        },
    )

    try:
        with urllib.request.urlopen(request, timeout=12) as response:
            response_body = response.read().decode("utf-8")
    except (TimeoutError, socket.timeout) as error:
        raise QwenCallError("qwen_timeout", str(error)) from error
    except urllib.error.URLError as error:
        raise QwenCallError("qwen_request_failed", str(error)) from error
    except OSError as error:
        raise QwenCallError("qwen_request_failed", str(error)) from error

    try:
        data = json.loads(response_body)
    except json.JSONDecodeError as error:
        raise QwenCallError(
            "qwen_json_parse_failed",
            str(error),
            qwen_raw_content_preview=preview_text(response_body),
        ) from error
    return parse_qwen_tool_call(data)


def parse_qwen_tool_call(data: Dict[str, Any]) -> ToolCall:
    choices = data.get("choices") or []
    if not choices:
        raise QwenCallError("qwen_no_tool_call", "Qwen response has no choices")
    message = choices[0].get("message") or {}

    tool_calls = message.get("tool_calls") or []
    if tool_calls:
        function = (tool_calls[0] or {}).get("function") or {}
        name = function.get("name")
        raw_arguments = function.get("arguments")
        try:
            arguments = parse_json_object(raw_arguments)
        except json.JSONDecodeError as error:
            raise QwenCallError(
                "qwen_json_parse_failed",
                str(error),
                qwen_tool_name=str(name or ""),
                qwen_raw_content_preview=preview_text(raw_arguments),
            ) from error
        if name and isinstance(arguments, dict):
            return ToolCall(
                name=name,
                arguments=arguments,
                source="qwen_tool_calls",
                qwen_tool_name=str(name),
                qwen_raw_content_preview=preview_text(raw_arguments),
            )
        if name:
            raise QwenCallError(
                "qwen_invalid_search_args",
                "Qwen tool call arguments were not a JSON object",
                qwen_tool_name=str(name),
                qwen_raw_content_preview=preview_text(raw_arguments),
            )

    content = message.get("content") or ""
    try:
        content_json = parse_json_object(content)
    except json.JSONDecodeError as error:
        raise QwenCallError(
            "qwen_json_parse_failed",
            str(error),
            qwen_raw_content_preview=preview_text(content),
        ) from error
    if isinstance(content_json, dict):
        name = content_json.get("tool") or content_json.get("name")
        arguments = content_json.get("arguments") or content_json.get("parameters") or {}
        if name and isinstance(arguments, dict):
            return ToolCall(
                name=str(name),
                arguments=arguments,
                source="qwen_content_json",
                qwen_tool_name=str(name),
                qwen_raw_content_preview=preview_text(content),
            )
        if name:
            raise QwenCallError(
                "qwen_invalid_search_args",
                "Qwen content JSON arguments were not an object",
                qwen_tool_name=str(name),
                qwen_raw_content_preview=preview_text(content),
            )

    text_tool_call = parse_qwen_text_tool_call(content)
    if text_tool_call is not None:
        return text_tool_call

    raise QwenCallError(
        "qwen_no_tool_call",
        "Qwen response did not contain a usable tool call",
        qwen_raw_content_preview=preview_text(content),
    )


def parse_qwen_text_tool_call(content: str) -> Optional[ToolCall]:
    if not isinstance(content, str) or "<function=" not in content:
        return None

    match = re.search(
        r"<function\s*=\s*([A-Za-z_][\w-]*)\s*>(.*?)</function\s*>",
        content,
        flags=re.DOTALL | re.IGNORECASE,
    )
    if not match:
        return None

    name = match.group(1).strip()
    body = match.group(2).strip()
    preview = preview_text(content)

    if name not in {"search_ads", "clarify_search"}:
        return ToolCall(
            name=name,
            arguments={},
            source="qwen_text_tool_call",
            qwen_tool_name=name,
            qwen_raw_content_preview=preview,
        )

    arguments = parse_text_tool_arguments(body, tool_name=name, preview=preview)
    return ToolCall(
        name=name,
        arguments=arguments,
        source="qwen_text_tool_call",
        qwen_tool_name=name,
        qwen_raw_content_preview=preview,
    )


def parse_text_tool_arguments(body: str, tool_name: str, preview: str) -> Dict[str, Any]:
    try:
        parsed = parse_json_object(body)
    except json.JSONDecodeError as error:
        raise QwenCallError(
            "qwen_json_parse_failed",
            str(error),
            qwen_tool_name=tool_name,
            qwen_raw_content_preview=preview,
        ) from error

    if isinstance(parsed, dict):
        return parsed

    parameter_args = parse_parameter_tags(body)
    if parameter_args:
        return parameter_args

    raise QwenCallError(
        "qwen_invalid_search_args",
        "Qwen text tool call did not contain JSON arguments or parameter tags",
        qwen_tool_name=tool_name,
        qwen_raw_content_preview=preview,
    )


def parse_parameter_tags(body: str) -> Dict[str, Any]:
    arguments: Dict[str, Any] = {}
    for match in re.finditer(
        r"<parameter\s*=\s*([A-Za-z_][\w-]*)\s*>(.*?)</parameter\s*>",
        body,
        flags=re.DOTALL | re.IGNORECASE,
    ):
        key = match.group(1).strip()
        value = match.group(2).strip()
        arguments[key] = parse_parameter_value(value)
    return arguments


def parse_parameter_value(value: str) -> Any:
    if not value:
        return ""
    try:
        return json.loads(value)
    except json.JSONDecodeError:
        return value


def parse_json_object(value: Any) -> Any:
    if isinstance(value, dict):
        return value
    if not isinstance(value, str):
        return None

    text = value.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text)
        text = re.sub(r"\s*```$", "", text)
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", text, flags=re.DOTALL)
        if match:
            return json.loads(match.group(0))
    return None


def preview_text(value: Any, max_length: int = 300) -> str:
    if value is None:
        return ""
    if isinstance(value, (dict, list)):
        text = json.dumps(value, ensure_ascii=False)
    else:
        text = str(value)
    text = re.sub(r"\s+", " ", text).strip()
    return text[:max_length]


def fallback_tool_call(
    query: str,
    current_channel: Optional[str],
    fallback_reason: str = "manual_fallback",
    qwen_tool_name: str = "",
    qwen_raw_content_preview: str = "",
) -> ToolCall:
    q = query.strip().lower()
    if not q or q in {"推荐", "推荐点东西", "看看", "随便看看", "有什么推荐", "广告推荐"}:
        return ToolCall(
            name="clarify_search",
            arguments={
                "question": "你更想看哪类广告？",
                "suggestedOptions": ["数码", "运动健身", "咖啡轻食", "本地优惠"],
            },
            source="fallback_parser",
            fallback_reason=fallback_reason,
            qwen_tool_name=qwen_tool_name,
            qwen_raw_content_preview=qwen_raw_content_preview,
        )

    args: Dict[str, Any] = {
        "intent": "BROWSE",
        "preferredChannel": None,
        "tags": [],
        "categories": [],
        "scenes": [],
        "audiences": [],
        "keywords": split_query_keywords(query),
        "excludeTags": [],
        "suggestedRefinements": [],
    }

    def has_any(words: List[str]) -> bool:
        return any(word in q for word in words)

    if is_cup_product_query(query):
        args.update(
            intent="PURCHASE",
            preferredChannel="ECOMMERCE",
            tags=["杯子", "水杯", "咖啡杯", "生活用品", "家居用品"],
            categories=["厨房用品", "生活用品", "家居用品"],
            keywords=CUP_PRODUCT_KEYWORDS,
            suggestedRefinements=CUP_PRODUCT_REFINEMENTS,
        )
    elif has_any(["手机", "耳机", "充电", "数码"]):
        args.update(
            intent="PURCHASE",
            preferredChannel="ECOMMERCE",
            tags=["数码", "性价比"],
            categories=["数码"],
            scenes=["通勤", "办公学习"],
            keywords=["手机", "耳机", "充电器", "移动电源", "数码"],
            suggestedRefinements=["只看数码", "学生党性价比", "通勤使用"],
        )
    elif has_any(["咖啡", "轻食", "奶茶", "周末", "附近", "优惠"]):
        args.update(
            intent="LOCAL_DEAL",
            preferredChannel="LOCAL",
            tags=["咖啡", "轻食", "周末", "优惠"],
            categories=["咖啡", "轻食", "本地生活"],
            scenes=["周末", "附近门店"],
            keywords=["咖啡", "轻食", "奶茶", "周末", "附近", "优惠"],
            suggestedRefinements=["附近优惠", "周末可用", "咖啡轻食"],
        )
    elif has_any(["健身", "跑步", "运动"]):
        args.update(
            intent="SCENE",
            preferredChannel=current_channel if current_channel in {"FEATURED", "LOCAL"} else "FEATURED",
            tags=["运动", "健身", "跑步"],
            categories=["运动"],
            scenes=["健身房", "城市跑步", "操场"],
            keywords=["健身", "跑步", "运动", "装备"],
            suggestedRefinements=["学生党运动装备", "跑步训练", "本地健身"],
        )
    elif has_any(["学生党", "性价比"]):
        args.update(
            intent="PURCHASE",
            preferredChannel=current_channel if current_channel in {"ECOMMERCE", "FEATURED"} else "ECOMMERCE",
            tags=["学生党", "性价比"],
            audiences=["学生党"],
            keywords=["学生党", "性价比", "实用"],
            suggestedRefinements=["学生党性价比", "通勤使用", "入门装备"],
        )
    elif has_any(["旅行", "露营"]):
        args.update(
            intent="SCENE",
            preferredChannel="FEATURED",
            tags=["旅行", "露营"],
            categories=["旅行", "户外"],
            scenes=["旅行", "露营", "周末"],
            keywords=["旅行", "露营", "户外", "周末"],
            suggestedRefinements=["周末露营", "旅行装备", "户外好物"],
        )
    elif has_any(["收纳", "家居"]):
        args.update(
            intent="PURCHASE",
            preferredChannel="ECOMMERCE",
            tags=["收纳", "家居"],
            categories=["家居"],
            scenes=["居家", "出租屋"],
            keywords=["收纳", "家居", "整理"],
            suggestedRefinements=["收纳用品", "小户型家居", "居家整理"],
        )

    args["explanation"] = build_default_explanation(query, args.get("preferredChannel"), args.get("keywords", []))
    return ToolCall(
        name="search_ads",
        arguments=args,
        source="fallback_parser",
        fallback_reason=fallback_reason,
        qwen_tool_name=qwen_tool_name,
        qwen_raw_content_preview=qwen_raw_content_preview,
    )


def ad_text(ad: Dict[str, Any], *fields: str) -> str:
    values = []
    for field in fields:
        value = ad.get(field)
        if isinstance(value, list):
            values.extend(str(item) for item in value)
        else:
            values.append(str(value or ""))
    return " ".join(values).lower()


def contains_any(haystack: str, needles: List[str]) -> bool:
    lower_haystack = haystack.lower()
    return any(needle.lower() in lower_haystack for needle in needles if needle)


def score_ad(ad: Dict[str, Any], args: Dict[str, Any]) -> int:
    score = 0
    preferred_channel = args.get("preferredChannel")
    tags = normalize_list(args.get("tags"))
    categories = normalize_list(args.get("categories"))
    scenes = normalize_list(args.get("scenes"))
    audiences = normalize_list(args.get("audiences"))
    keywords = normalize_list(args.get("keywords"))
    exclude_tags = normalize_list(args.get("excludeTags"))

    if preferred_channel and ad.get("channel") == preferred_channel:
        score += 8

    ad_tags = normalize_list(ad.get("tags"))
    for tag in tags:
        if any(tag.lower() == ad_tag.lower() or tag.lower() in ad_tag.lower() or ad_tag.lower() in tag.lower() for ad_tag in ad_tags):
            score += 20

    category_text = ad_text(ad, "category")
    for category in categories:
        if category.lower() in category_text:
            score += 16

    scene_text = ad_text(ad, "scene")
    for scene in scenes:
        if scene.lower() in scene_text:
            score += 12

    audience_text = ad_text(ad, "audience", "targetAudience")
    for audience in audiences:
        if audience.lower() in audience_text:
            score += 12

    title_text = ad_text(ad, "title")
    brand_text = ad_text(ad, "brand", "brandName")
    summary_text = ad_text(ad, "summary", "description")
    reason_text = ad_text(ad, "recommendationReason")
    for keyword in keywords:
        key = keyword.lower()
        if key in title_text:
            score += 12
        if key in brand_text:
            score += 8
        if key in summary_text:
            score += 8
        if key in reason_text:
            score += 6

    for exclude_tag in exclude_tags:
        if any(exclude_tag.lower() in ad_tag.lower() or ad_tag.lower() in exclude_tag.lower() for ad_tag in ad_tags):
            score -= 30

    return score


def relaxed_score_ad(ad: Dict[str, Any], args: Dict[str, Any]) -> int:
    keywords = normalize_list(args.get("keywords"))
    if not keywords:
        return 0
    text = ad_text(
        ad,
        "title",
        "brand",
        "brandName",
        "summary",
        "description",
        "recommendationReason",
        "category",
        "scene",
        "audience",
        "targetAudience",
        "tags",
        "keywords",
    )
    return sum(4 for keyword in keywords if keyword.lower() in text)


def product_term_match_level(ad: Dict[str, Any], profile: Dict[str, Any]) -> str:
    text = ad_text(
        ad,
        "title",
        "brand",
        "brandName",
        "summary",
        "description",
        "recommendationReason",
        "category",
        "scene",
        "audience",
        "targetAudience",
        "tags",
        "keywords",
    )
    if contains_any(text, normalize_list(profile.get("directTerms"))):
        return "direct"
    if contains_any(text, normalize_list(profile.get("relatedTerms"))):
        return "related"
    return ""


def execute_search_ads(args: Dict[str, Any], limit: int, query: str = "") -> Tuple[List[str], str]:
    scored: List[Tuple[Dict[str, Any], int, str]] = []
    product_profile = explicit_product_profile(query)
    for ad in ADS:
        score = score_ad(ad, args)
        product_match_level = ""
        if product_profile is not None:
            product_match_level = product_term_match_level(ad, product_profile)
            if not product_match_level:
                continue
            score += 40 if product_match_level == "direct" else 16
        if score > 0:
            scored.append((ad, score, product_match_level))

    match_level = ""
    if product_profile is not None and scored:
        direct_scored = [item for item in scored if item[2] == "direct"]
        if direct_scored:
            scored = direct_scored
            match_level = "direct"
        else:
            match_level = "related"
            limit = min(limit, 6)

    if len(scored) < min(limit, 3) and product_profile is None:
        seen_ids = {str(ad.get("id")) for ad, _, _ in scored}
        for ad in ADS:
            ad_id = str(ad.get("id"))
            if ad_id in seen_ids:
                continue
            score = relaxed_score_ad(ad, args)
            if score > 0:
                scored.append((ad, score, ""))

    if product_profile is not None and not scored:
        match_level = "fallback"
        for ad in ADS:
            score = relaxed_score_ad(ad, args)
            if score > 0:
                scored.append((ad, score, "fallback"))
        limit = min(limit, 4)

    if not match_level:
        match_level = "direct" if scored else "fallback"

    scored.sort(
        key=lambda item: (
            -item[1],
            -int(item[0].get("likeCount") or 0),
            str(item[0].get("id") or ""),
        )
    )
    return [str(ad.get("id")) for ad, _, _ in scored[:limit] if str(ad.get("id")) in ADS_BY_ID], match_level


def debug_payload(tool_call: ToolCall) -> Dict[str, str]:
    return {
        "source": tool_call.source,
        "fallbackReason": tool_call.fallback_reason,
        "qwenToolName": tool_call.qwen_tool_name,
        "qwenRawContentPreview": preview_text(tool_call.qwen_raw_content_preview),
    }


@app.get("/health")
def health() -> Dict[str, Any]:
    return {
        "status": "ok",
        "adCount": len(ADS),
        "qwenBaseUrl": QWEN_BASE_URL,
        "qwenModel": QWEN_MODEL,
    }


@app.post("/api/ai-search")
def ai_search(request: AiSearchRequest) -> Dict[str, Any]:
    query = request.query.strip()
    current_channel = request.currentChannel if request.currentChannel in VALID_CHANNELS else None
    limit = max(1, min(request.limit, 50))

    try:
        tool_call = call_qwen(query, current_channel, limit)
    except QwenCallError as error:
        tool_call = fallback_tool_call(
            query,
            current_channel,
            fallback_reason=error.reason,
            qwen_tool_name=error.qwen_tool_name,
            qwen_raw_content_preview=error.qwen_raw_content_preview,
        )
    except Exception as error:
        tool_call = fallback_tool_call(
            query,
            current_channel,
            fallback_reason="qwen_request_failed",
            qwen_raw_content_preview=str(error),
        )

    if tool_call.name == "clarify_search" and has_query_hint(query, PURCHASE_HINTS + LOCAL_HINTS):
        tool_call = fallback_tool_call(
            query,
            current_channel,
            fallback_reason="qwen_returned_clarify_for_obvious_query",
            qwen_tool_name=tool_call.qwen_tool_name or tool_call.name,
            qwen_raw_content_preview=tool_call.qwen_raw_content_preview,
        )

    if tool_call.name == "clarify_search":
        return {
            "type": "clarify",
            "query": request.query,
            "question": str(tool_call.arguments.get("question") or "你更想看哪类广告？"),
            "suggestedOptions": normalize_list(tool_call.arguments.get("suggestedOptions"))[:6]
            or ["数码", "运动健身", "咖啡轻食", "本地优惠"],
            "debug": debug_payload(tool_call),
        }

    if tool_call.name != "search_ads":
        tool_call = fallback_tool_call(
            query,
            current_channel,
            fallback_reason="qwen_unknown_tool",
            qwen_tool_name=tool_call.qwen_tool_name or tool_call.name,
            qwen_raw_content_preview=tool_call.qwen_raw_content_preview,
        )

    args = sanitize_search_args(tool_call.arguments, query, current_channel)
    matched_ad_ids, match_level = execute_search_ads(args, limit, query=query)
    args["suggestedRefinements"] = build_suggested_refinements(args)
    args["explanation"] = build_explanation(
        query=query,
        channel=args["preferredChannel"],
        keywords=args["keywords"],
        model_explanation=args["explanation"],
        match_level=match_level,
    )

    return {
        "type": "results",
        "query": request.query,
        "explanation": args["explanation"],
        "matchedAdIds": matched_ad_ids,
        "matchLevel": match_level,
        "toolArguments": {
            "intent": args["intent"],
            "preferredChannel": args["preferredChannel"],
            "tags": args["tags"],
            "categories": args["categories"],
            "scenes": args["scenes"],
            "audiences": args["audiences"],
            "keywords": args["keywords"],
            "excludeTags": args["excludeTags"],
        },
        "suggestedRefinements": args["suggestedRefinements"],
        "source": tool_call.source,
        "debug": debug_payload(tool_call),
    }
