# AI Search Backend

This is a lightweight FastAPI backend for the Android AI ad search demo. It reads the same ad pool used by the app:

```text
app/src/main/assets/mock_ads.json
```

Qwen is expected to expose an OpenAI-compatible API. Defaults are provided when environment variables are not set.

## Start

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
QWEN_BASE_URL=http://localhost:8001/v1 QWEN_MODEL=qwen QWEN_API_KEY=EMPTY uvicorn ai_search_server:app --host 0.0.0.0 --port 8000
```

Android Emulator usually accesses the host machine with:

```text
http://10.0.2.2:8000
```

For a real device, replace it with your computer's LAN IP, for example:

```text
http://192.168.x.x:8000
```

## API

```http
GET /health
POST /api/ai-search
```

Request:

```json
{
  "query": "买个手机",
  "currentChannel": "ECOMMERCE",
  "limit": 12
}
```

The backend asks Qwen to choose either `search_ads` or `clarify_search`. Qwen only produces structured arguments. The backend executes the actual local filtering, scoring, sorting, and returns real ad IDs from `mock_ads.json`.
