# OpenGL 06 - Shaders e modelos MD2

## Objetivo do trabalho

O enunciado pede para integrar o carregamento e a renderização de modelos MD2
com shaders. A preferência é interpolar os frames da animação no **vertex
shader**, para que o movimento entre um frame e o seguinte fique suave.

Em outras palavras, o programa precisa ler a geometria animada do arquivo MD2,
enviar os vértices para a placa de vídeo e desenhar o modelo com uma textura.
Durante a animação, cada vértice deve passar gradualmente da posição que tem em
um frame para a posição correspondente no próximo.

## Como rodar

Abra um terminal nesta pasta (a pasta que contém este `README.md`) e execute:

```sh
mvn exec:java
```

Sem argumentos, o programa mostra o jatinho X-35, que é o exemplo OBJ da cena.
Para executar a demonstração de animação MD2, informe um arquivo `.md2` e,
opcionalmente, o caminho da imagem usada como textura:

```sh
mvn exec:java -Dexec.args="caminho/modelo.md2 caminho/skin.png"
```

O modelo MD2 e sua textura são arquivos separados. Se a textura não for
informada, o programa tenta usar `texturaGato.jpeg`. Não há um modelo `.md2`
incluído neste projeto: é necessário fornecer um arquivo MD2 válido para testar
essa demonstração.

Requer Java 8 ou superior, Maven e Linux x64; as dependências nativas do LWJGL
estão configuradas para Linux.

Para apenas compilar, sem abrir a janela, use:

```sh
mvn package
```

## Como a implementação funciona

### 1. Leitura do arquivo MD2

`Model/Md2Model.java` lê o arquivo em formato binário. O carregador verifica o
cabeçalho e a versão do formato, depois lê:

- os vértices de cada frame da animação;
- os triângulos, que indicam quais vértices formam a superfície;
- as coordenadas UV, usadas para posicionar a textura sobre os triângulos.

No MD2, cada posição de vértice é armazenada de forma compacta. O carregador
usa os valores `scale` e `translate` de cada frame para convertê-la para uma
posição real. Também converte o eixo vertical do MD2 para o eixo usado pela
cena.

### 2. Preparação dos buffers da GPU

Para desenhar um triângulo, o OpenGL precisa receber os vértices na ordem em que
serão renderizados. O carregador expande os índices dos triângulos em listas de
posições e associa cada canto à sua coordenada UV.

São usados dois buffers de posição:

- **buffer A** contém as posições do frame atual;
- **buffer B** contém as posições do próximo frame.

As coordenadas de textura ficam em um terceiro buffer. Quando a animação avança
para outros frames, a CPU atualiza A e B; os buffers só são atualizados quando o
número do frame muda.

### 3. Interpolação no vertex shader

`shaders/md2VertexShader.txt` recebe as duas posições (`positionA` e
`positionB`) e um valor `blend`, entre 0 e 1. Esta linha calcula uma posição
intermediária:

```glsl
vec3 position = mix(positionA, positionB, blend);
```

Com `blend = 0`, o vértice está no frame A; com `blend = 1`, está no frame B.
Entre esses valores, a GPU calcula uma transição gradual. O programa atualiza
`blend` continuamente com o tempo decorrido, fazendo a animação avançar.

Depois, o vertex shader aplica as matrizes do modelo, da câmera e da projeção
para posicionar o vértice na tela.

### 4. Textura e desenho

O fragment shader (`shaders/md2FragmentShader.txt`) usa as coordenadas UV para
consultar a textura e definir a cor dos pixels do modelo. Assim, o fluxo é:

```text
arquivo MD2 -> vértices e UVs -> buffers A/B -> interpolação no vertex shader
            -> textura no fragment shader -> imagem na tela
```

## Como estudar e testar

Uma boa forma de entender o processo é acompanhar `Md2Model.update()`, que
escolhe os frames e calcula `blend`; depois `Md2Model.draw()`, que envia os
buffers; e, por fim, a função `main` do vertex shader, que mistura as posições.
As teclas **W/S** e **A/D** giram a cena, **Z/X** ajustam o zoom e **Escape**
fecha a janela.

O carregador usa os triângulos e a ordem de vértices definidos pelo próprio
arquivo MD2. É uma implementação didática para demonstrar o pipeline e a
interpolação, não um sistema completo de edição ou gerenciamento de animações
MD2 nomeadas.
