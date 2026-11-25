from ragservice.infrastructure.config.settings import getSettings
from ragservice.infrastructure.embedding.e5EmbeddingAdapter import E5EmbeddingAdapter
from ragservice.infrastructure.embedding.e5Model import loadE5Model


# Baixa o modelo na revisão configurada para que o serviço inicie sem acessar a rede
def prepareModel() -> None:
    settings = getSettings()
    model = loadE5Model(settings, localFilesOnly=False)
    adapter = E5EmbeddingAdapter(model, settings.embeddingBatchSize)
    adapter.ensurePassageLimit(settings.chunkMaxTokens)
    adapter.embedPassages(["Verificação do modelo de embeddings."])

    print(f"Modelo: {settings.embeddingModelName}")
    print(f"Revisão: {settings.embeddingModelRevision}")
    print(f"Dimensão: {adapter.dimension}")
    print(f"Limite de tokens por trecho: {adapter.maxPassageTokens()}")


if __name__ == "__main__":
    prepareModel()
