from fastapi.testclient import TestClient

from ragservice.main import app


def testLiveReportsRunningService() -> None:
    with TestClient(app) as client:
        response = client.get("/health/live")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def testReadyReportsServiceCannotAnswerQueriesYet() -> None:
    with TestClient(app) as client:
        response = client.get("/health/ready")

    assert response.status_code == 503
    assert response.json() == {"status": "not_ready"}
