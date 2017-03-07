# Project's goal

It serves a role of playground project for libraries testing and various experimentations. It can be also considered
 a bootstrap for future more serious projects but it's not prepared as project seed. So it should be reused by
 choosing only interesting snippets and copying them to target project (as opposed to straightforward git clone
 or directory copying).
 
# How to run it

It uses sbt-revolver so from sbt console:

```
web/re-start
```

Gatling tests can be run with:

```
gatling/gatling-it:test
```

Scala console inside `test` task is run with Ammonite, so e.g. `web/test:console` will be run with Ammonite.

`-Xfatal-warnings` may be annoying for running REPL, you can disable it per sbt session with following in sbt console:

```
set scalacOptions in web ~= { options => options.filterNot(_ == "-Xfatal-warnings") }
```