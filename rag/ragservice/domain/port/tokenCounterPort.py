from abc import ABC, abstractmethod


class TokenCounterPort(ABC):

    @abstractmethod
    def countTokens(self, texts: list[str]) -> list[int]:
        pass
