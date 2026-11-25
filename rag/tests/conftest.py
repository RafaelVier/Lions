import os

import pytest

RUN_MODEL_TESTS_VARIABLE = "RAG_RUN_MODEL_TESTS"


def pytest_configure(config: pytest.Config) -> None:
    config.addinivalue_line("markers", "model: testes que carregam o modelo real de embeddings")


def pytest_collection_modifyitems(config: pytest.Config, items: list[pytest.Item]) -> None:
    if os.environ.get(RUN_MODEL_TESTS_VARIABLE) == "1":
        return

    skipModel = pytest.mark.skip(reason=f"Defina {RUN_MODEL_TESTS_VARIABLE}=1 para usar o modelo real.")
    for item in items:
        if "model" in item.keywords:
            item.add_marker(skipModel)
