Scalit - Literate Programming for Scala
=======================================
For my semester project at EPFL in 2008 or so,
I wrote about literate programming in Scala. The coding part consisted in
implementing the format chosen by the excellent [noweb](http://www.cs.tufts.edu/~nr/noweb/) tool and bring some compiler support to scala (so that you can,
for example, reference line numbers in the literate file, and of course get
some nicely formatted scala code in your (La)TeX documents).

Currently, bootstrapping works on the latest Scala, but there is an issue -
the LitComp object, used to directly compile from nw files, doesn't work any
more, so I'll also have to adapt the buildfile, and then rewrite LitComp!

Cheers,

Sebastian Gfeller (sebug.ch)



