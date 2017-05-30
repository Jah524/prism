# Prism

[![Build Status](https://travis-ci.org/Jah524/prism.svg?branch=master)](https://travis-ci.org/Jah524/prism)

Prism is a handy neural network library for natural language processing written in pure Clojure.
This library get you a distributed representation of words.
Now prism is ready to work with following models.

- Word2Vec by skip-gram with negative-sampling
- RNNLM with negative-sampling

This library also includes some basic neural network model (e.g. feedforward, LSTM).
See demonstration section and [examples](/src/examples) for more detail.

## Usage

`git clone https://github.com/Jah524/prism.git` and `lein repl`.
You can find how to do with prism at [project wiki](https://github.com/Jah524/prism/wiki)

If you want to work on trained model in your project, add following dependency to your `project.clj`,

```
[jah524/prism "0.5.1"]
```

## Demonstration

### Feed Forward

- approximation of sin function with 3 hidden units

```
lein run -m  examples.feedforward.sin3
```

### LSTM

- with dense input

```
lein run -m examples.lstm.simple-prediction
```

- with sparse inputs

```
lein run -m examples.lstm.sparse
```

- multi class classification

```
lein run -m examples.lstm.multi-class
```

## License

Copyright © 2017 Jah524

Distributed under the Eclipse Public License either version 1.0

