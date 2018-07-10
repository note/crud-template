# Pragmatic functional programming in Scala

In this branch there's a code with gaps that need to be filled. If you want to see the fully
working version of the code switch to `demo-with-solutions` branch.

## How to run presentation

Run the following:

```bash
cd presentation && npm install && node plugin/notes-server
```

Then follow navigate to returned URL to view the presentation.

## How to run the code

### Running application

```
./dockerized_sbt.sh
```

Then withing sbt shell:

```
reStart
```

### How to run tests

#### Unit tests

From sbt shell:

```
test
```

#### Integration tests:

From sbt shell:

```
web/fun:test
```
