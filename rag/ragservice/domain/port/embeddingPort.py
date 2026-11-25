from abc import ABC, abstractmethod

import numpy as np


class EmbeddingError(ValueError):
    pass


class EmbeddingPort(ABC):

    @property
    @abstractmethod
    def dimension(self) -> int:
        pass

    @abstractmethod
    def embedPassages(self, texts: list[str]) -> np.ndarray:
        pass

    @abstractmethod
    def embedQuery(self, text: str) -> np.ndarray:
        pass
