# Pragmatic functional programming in Scala

## How to run presentation

Run the following:

```bash
npm install && node plugin/notes-server
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
