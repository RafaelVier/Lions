from ragservice.domain.port.tokenCounterPort import TokenCounterPort


class E5TokenCounter(TokenCounterPort):

    def __init__(self, tokenizer):
        self.tokenizer = tokenizer

    def countTokens(self, texts: list[str]) -> list[int]:
        if not texts:
            return []
        encoded = self.tokenizer(texts, add_special_tokens=False)
        return [len(inputIds) for inputIds in encoded["input_ids"]]
