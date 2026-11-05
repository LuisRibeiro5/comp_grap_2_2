# OpenGL 06 - Shaders e modelos MD2

Projeto didatico Java/LWJGL para carregar modelos OBJ e MD2 e desenha-los com shaders.

## Compilar

Requer Java 8 ou superior e Maven:

```sh
mvn package
```

## Executar um modelo MD2

No Eclipse, execute a classe `Main3D` com estes argumentos:

```text
caminho/modelo.md2 caminho/skin.png
```

O segundo argumento e opcional; sem ele, o programa tenta usar `texturaGato.jpeg`.
O arquivo MD2 contem a geometria animada, enquanto a skin e uma imagem separada.
Sem argumentos, o programa segue o exemplo OBJ original.

W/S e A/D giram a cena, Z/X alteram o zoom e Escape fecha a janela.

## Como funciona a interpolacao

O carregador le os triangulos, coordenadas UV e vertices compactados de cada frame
MD2. Para cada triangulo, ele prepara dois buffers de vertices (frame atual e
seguinte). O vertex shader mistura essas posicoes usando `blend`; assim a GPU
faz a interpolacao entre os frames antes da projecao e renderizacao.
