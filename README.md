# Alex's Caves x Tectonic Compat (NeoForge 1.21.1)

Mod de compatibilidade entre o **Alex's Caves (Unofficial Port, Raguto, 2.0.10)** e o **Tectonic 3.x**.

## O que ele faz

1. **Biomas** – Mixin no `RETURN` de `MultiNoiseBiomeSource.getNoiseBiome`. Quando o Alex's Caves
   não devolve bioma (porque as faixas de clima falharam com o terreno do Tectonic), o mod consulta o
   **mapa Voronoi do próprio AC** (`ACBiomeRarity.getACBiomeForPosition`) e devolve o bioma certo,
   respeitando: biomas desativados na config do AC, distância do spawn, profundidade mínima
   (`depth ≥ 0.05`, igual ao AC) e, para o Abyssal Chasm, a exigência de estar sob oceano.
2. **Formato das cavernas** – As cavernas do AC são esculpidas por estruturas (`ferrocave`,
   `dino_bowl`, `acid_pit`, `forlorn_canyon`, `cake_cave`, `ocean_trench`) que usam esse **mesmo**
   mapa Voronoi para tamanho e posição. Elas só nascem se o bioma no ponto inicial for o do AC – o
   item 1 garante isso, então bioma e caverna ficam alinhados.
3. **Proteção contra o Tectonic** – Uma density function (`ac_tectonic_compat:ac_zone`) e quatro
   `worldgen_modifier` do Lithostitched desligam, **só dentro das zonas do AC**, as cavernas
   cheese/noodle, os rios subterrâneos e os túneis de lava do Tectonic. Assim eles não abrem buracos
   no meio das cavernas redondas do AC.
4. **Garantia de `possibleBiomes`** – Registra os biomas do AC no BiomeSource do Overworld antes da
   criação do mundo (senão o jogo descarta as estruturas do AC e o `/locate biome` nem procura).

## Compilar

1. Pegue o MDK do NeoForge 1.21.1 (ModDevGradle) e copie para esta pasta: `gradlew`, `gradlew.bat`
   e a pasta `gradle/` (o wrapper).
2. Coloque em `libs/` os jars do seu modpack:
   - `alexscaves-*.jar` (Unofficial Port do Raguto)
   - `citadel-*.jar` (Unofficial Port)
3. Rode `./gradlew build` (ou `gradlew.bat build` no Windows).
4. O jar sai em `build/libs/ac_tectonic_compat-0.1.0.jar`. Jogue na pasta `mods` do pack.

Dependências em jogo: Alex's Caves (port), Tectonic 3.x (versão **mod**, não datapack) e Lithostitched.

## Testar (num mundo NOVO)

- No log do servidor procure `AC-Tectonic-Compat`. Deve aparecer
  `OK: biomas e estruturas de caverna do Alex's Caves ativos no Overworld.`
- `/actcompat find` – lista as zonas do AC mais próximas (lê só o Voronoi, não gera chunks).
- Vá até uma delas, desça para Y ~0 e use `/actcompat info`. Mostra seed, se o BiomeSource está
  registrado, se as estruturas do AC estão ativas, o Voronoi, os valores de clima e o bioma final.
- `/locate biome alexscaves:magnetic_caves` deve funcionar agora.

## Config (`config/ac_tectonic_compat-common.toml`)

| chave | padrão | o que faz |
|---|---|---|
| `biome_fallback` | true | liga o item 1 |
| `min_depth` / `max_depth` | 0.05 / 2.0 | faixa de profundidade do bioma |
| `abyssal_max_continentalness` | -0.19 | quão "oceano" precisa ser para o Abyssal Chasm |
| `protect_ac_caves` | true | liga o item 3 |
| `zone_min_y` / `zone_max_y` | -64 / 72 | faixa Y onde a proteção vale |

## Limitações conhecidas

- Não foi compilado nem testado em jogo ainda: os nomes de classes/métodos do AC foram conferidos no
  código-fonte do port (GitHub Raguto/AlexsCaves-1.21.1) e os do Tectonic na v3.0.25; os do Minecraft
  são Mojmap 1.21.1. Se algo não compilar, o erro aponta a linha exata.
- Com `biome_fallback` ligado, as faixas de **continentalidade** do AC deixam de valer (exceto o
  Abyssal Chasm). Primordial/Toxic/Candy podem aparecer em áreas que o AC original evitaria.
- Vale só para chunks gerados com o mod instalado.

Licença: GPL-3.0 (o mod chama código GPL do Alex's Caves).

## 0.2.0 – corrige mudanças do port

- **Pedras dos biomas** (radrock, galena, guanostone…): o port só convertia a pedra até Y 50.
  Agora volta à regra original: converte onde o *bioma* é do AC, em toda a altura.
- **Estruturas fora do bioma**: o port permitia as estruturas em qualquer bioma do Overworld
  (`#alexscaves:has_cave_structures`). Voltam a exigir o bioma certo, como no original.
- **Forlorn Hollows**: volta o formato original de cânion (o port trocou por uma "tigela") e
  volta a gerar cânions em todo o bioma, não só a 3 chunks do centro. Espaçamento original (6/4).
- **Árvores** com a densidade original (o port comprimiu a faixa de altura sem reduzir a quantidade).

Também recomendado: em `config/alexscaves-general.toml`, voltar aos valores do original
`cave_biome_mean_width = 300.0` e `cave_biome_mean_separation = 900` (o port usa 160 e 2000).
