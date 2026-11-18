from fastapi import FastAPI, status
from fastapi.responses import JSONResponse

app = FastAPI(title="Lions RAG")


@app.get("/health/live")
def live() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/health/ready", status_code=status.HTTP_503_SERVICE_UNAVAILABLE)
def ready() -> JSONResponse:
    return JSONResponse(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        content={"status": "not_ready"},
    )
