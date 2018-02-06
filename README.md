# Cornflakes
A powerful yet simple JVM language with a compiler written in Java.

# Why is it called Cornflakes?
While I was writing the basics for the compiler, I took a moment to think about what I should name the language.
I had this song stuck in my head, written by comedian and rapper [Michael Dapaah, or Big Shaq](https://en.wikipedia.org/wiki/Michael_Dapaah),
entitled [_Man's Not Hot_](https://en.wikipedia.org/wiki/Man%27s_Not_Hot).  I had this one line stuck in my head especially,

> Movin' that cornflakes, Rice Krispies

I wasn't sure what to name the language, so I just decided to give it the working name Cornflakes. Eventually, it just became the final name of the project.

When Dapaah was asked about this quote, he responded with this extremely eloquent response:

> Big man don’t try to incriminate me, fam, ‘cause I’m talking about cereal. Man trap on the phone and move Corn Flakes. Rice Krispies. Coco Pops. Weetabix. Man don’t try and incriminate me. FBI is watching. I’m in the States, fam. You get me? FBI, CSI, all of you man. Yeah. Flipping Jackie Chan Rush Hour 4 cops. All of you lot, fam. I’m talking about cereal, fam. It’s a cereal thing. That’s what I’m talking ‘bout.
[credit [Genius](https://genius.com/12964946)]

...whatever that means.

# How does it compare to Java?
Cornflakes supports all features of Java (except for, at the time of this being written, lambda expressions).
On top of this, it has several more features. These features include, but are not limited to the following:

* [Operator Overloading](https://github.com/LucasBaizer/Cornflakes/wiki/Operator-Overloading)
* [Array Indexing of Objects](https://github.com/LucasBaizer/Cornflakes/wiki/Indexer-Functions)
* [Referencing of Getters and Setters like Variables](https://github.com/LucasBaizer/Cornflakes/wiki/Variable-Syntax-Function-References)
* [Pointers](https://github.com/LucasBaizer/Cornflakes/wiki/Pseudopointers)
* [Tuples](https://github.com/LucasBaizer/Cornflakes/wiki/Tuples)

...and much, much more.

# Documentation
Documentation for Cornflakes syntax can be found on the repository's [wiki page](https://github.com/LucasBaizer/Cornflakes/wiki).
Documentation for the Cornflakes standard library can be found on the JavaDoc located at the [github.io page](https://lucasbaizer.github.io/Cornflakes/).

# Contributing
If you're reading this and are interested in contributing to the project, I would love to check out (pun intended) your PR's.
The Cornflakes tokenizer is quite limiting due to its simplicity (see [this class](src/cornflakes/compiler/GenericBodyCompiler.java)).
It currently does not support some nice features (and necessities), such as parentheses in mathematical and boolean expressions.
It also prevents the declaration of lambdas and anonymous classes, both useful additions. Creating the parsing methodology for this
without rewriting much else would be ideal. I will get around to this once most language features are added, though.

# License
Cornflakes is licensed under the [MIT License](LICENSE).
