# OpenGL 07 - Movimentacao e colisao

## Objetivo do trabalho

Esta parte do projeto acrescenta movimentacao da nave, disparo de projeteis e
colisao com os cubos da cena. Quando um tiro acerta um cubo, o projetil some e o
cubo toca uma animacao curta de explosao antes de desaparecer.

## Como rodar

Abra um terminal nesta pasta (a pasta que contem este `README.md`) e execute:

```sh
mvn exec:java
```

O comando compila e inicia o programa. Requer Java 8 ou superior, Maven e Linux
x64; as dependencias nativas do LWJGL estao configuradas para Linux.

Para apenas compilar, sem abrir a janela:

```sh
mvn package
```

## Controles

- **W / S**: mover a nave para frente / para tras.
- **A / D**: mover a nave para a esquerda / para a direita.
- **E / Q**: mover a nave para cima / para baixo (eixo Y).
- **Espaco**: atirar. Enquanto a tecla estiver pressionada, a nave dispara.
- **Escape**: fechar a janela.

Cada disparo cria dois projeteis, um de cada lado da nave. Os cubos sao
distribuidos pela cena; use os controles para alinhar os tiros com eles.

## Como a implementacao funciona

### 1. Movimento e disparo

`obj/Personagem.java` representa a nave. No metodo `SimulaSe`, as teclas de
movimento alteram suas coordenadas ao longo do tempo: A/D mudam X, E/Q mudam Y
e W/S mudam Z. Quando Espaco esta pressionado e o intervalo entre disparos
passou de 100 ms, sao criados dois objetos `Projetil` e adicionados a lista de
projeteis da cena.

Cada projetil guarda sua posicao e velocidade. A cada quadro, `Main3D` chama
`Projetil.SimulaSe`, que atualiza a posicao. O projetil tambem tem um tempo de
vida maximo para nao permanecer na cena indefinidamente.

### 2. Detectar o impacto

Depois de atualizar o movimento, `Main3D` compara cada projetil com os cubos.
O teste fica no metodo `Cubo3D.colideCom` e usa uma aproximacao por esferas:

1. Calcula a diferenca entre as coordenadas X, Y e Z do cubo e do projetil.
2. Soma os tamanhos dos dois objetos para obter uma distancia de colisao.
3. Compara a distancia entre os centros com essa distancia limite.

Em forma simples, a colisao acontece quando:

```text
dx*dx + dy*dy + dz*dz <= raioColisao*raioColisao
```

Usamos uma esfera para simplificar o teste entre objetos 3D. O fator `sqrt(3)`
ajuda a esfera aproximada a cobrir os cantos do cubo. Portanto, nao e uma
colisao exata triangulo a triangulo, mas e simples e suficiente para este
exercicio.

### 3. Animacao e remocao do cubo

Quando `Cubo3D.colideCom(projetil)` retorna `true`, o loop em `Main3D` remove
o projetil de `listaProjeteis` e chama `explodir()` no cubo. O metodo marca o
cubo como `explodindo` e zera seu cronometro.

A cada quadro, `Cubo3D.SimulaSe` soma o tempo decorrido ao cronometro. Enquanto
os 250 ms da animacao nao passaram, aumenta o tamanho do cubo (`raio`), criando
o efeito visual de explosao. Ao final, marca `vivo = false`. O loop de
atualizacao em `Main3D` remove da lista os cubos que nao estao mais vivos. Assim,
o tiro some no impacto e o cubo permanece visivel apenas durante a animacao.

Esta explosao e propositalmente simples: o cubo cresce por um instante e some;
nao sao criadas particulas ou fragmentos.

Os loops que removem itens percorrem as listas de tras para frente. Isso evita
pular o proximo elemento quando um item e retirado de uma lista durante a
iteracao.

## Onde estudar o codigo

- `main/Main3D.java`: atualizacao da cena, verificacao de colisao e remocao.
- `obj/Personagem.java`: movimento da nave e criacao dos tiros.
- `obj/Projetil.java`: movimento e tempo de vida do projetil.
- `obj/Cubo3D.java`: teste de colisao e animacao de explosao.
