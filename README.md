# NT Grego — widget do versículo do dia

Widget Android que mostra um trecho do **Novo Testamento em grego** (SBLGNT) por dia.

**Seleção:** 1.500 trechos de 1 a 3 versículos que formam **frases completas** (pela pontuação do SBLGNT), escolhidos entre os mais citados nas referências cruzadas do OpenBible.info. Cada trecho aparece uma vez antes de repetir (~4 anos). Scripts em `tools/`.
Tocar no widget abre o leitor: **grego clicável** (toque numa palavra → análise morfológica + glosa), e a **TB** embutida (funciona offline).

- ↻ no widget: sorteia outro versículo para hoje
- ‹ › : versículo anterior/seguinte · ☀ : volta ao versículo do dia · ⋮ : aparência do widget, abrir no YouVersion
- ↗ : abre o versículo no YouVersion

**Instalar:** baixe `NTGrego.apk` na aba *Releases* deste repositório.

Texto grego: *The Greek New Testament: SBL Edition* © 2010 Society of Biblical Literature e Logos Bible Software (via MorphGNT), uso conforme a [licença SBLGNT](https://sblgnt.com/license/).

Referências cruzadas: [OpenBible.info](https://www.openbible.info/labs/cross-references/), CC-BY.

**Nova versão:** altere o número em `VERSION` e faça push — o GitHub compila e publica a release.

## Dados embutidos (`app/src/main/assets`)
| Arquivo | Conteúdo | Fonte / licença |
|---|---|---|
| `nt.txt`, `morph.txt` | Texto grego e morfologia | SBLGNT (EULA SBL) + MorphGNT (CC-BY-SA) |
| `lex.txt` | Glosas em inglês por lema | Dodson Greek Lexicon (CC0) |
| `tb.txt` | Tradução Brasileira | edição SBB 2010 via damarals/biblias — **uso pessoal**; a SBB reivindica direitos sobre esta edição (só a de 1917 é domínio público) |
| `units.txt` | Trechos do dia | gerado por `tools/units.py` |

Regerar dados: `tools/build_data.py`.
