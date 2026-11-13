# Projetos de Computacao Grafica

Este repositorio contem dois projetos independentes:

- [`opengl06/`](./opengl06/): shaders e carregamento/renderizacao de modelos,
  incluindo o exemplo MD2.
- [`opengl07_movimentacao/`](./opengl07_movimentacao/): projeto separado de
  movimentacao.

Cada pasta tem seu proprio `pom.xml`. A partir da raiz do repositorio, entre na
pasta do projeto antes de executar para que ele encontre os modelos e texturas.
Para rodar o OpenGL 06:

```sh
cd opengl06
mvn exec:java
```

Para rodar o OpenGL 07:

```sh
cd opengl07_movimentacao
mvn exec:java
```

`mvn package` apenas compila o projeto; para abrir a janela, use `exec:java`.
Os mesmos comandos tambem estao no README de cada projeto.

O Git e compartilhado na raiz deste repositorio; os projetos continuam separados
por diretorio e podem evoluir de forma independente.
