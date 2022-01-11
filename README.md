# KoRAL

The *Ko*tlin *R*egister *A*utomata *L*ibrary is an extension to [AutomataLib](https://learnlib.de/projects/automatalib/)
that provides

* a data model for extended automata, i.e., automata that combine a FSA-automaton-style graph structure with a memory
  model,
* a data model for register automata, i.e., EAs that only possess a finite set of registers,
* deserialization support for a [DOT](https://graphviz.org/doc/info/lang.html) subset,
* deserialization support for [Automata Wiki](https://automata.cs.ru.nl/) register automata,
* a RA random generator,
* non-emptiness testing by backwards propagation for succinct RAs, and
* various utilities.

At the moment, Koral is still experimental and APIs etc. are likely to change.
