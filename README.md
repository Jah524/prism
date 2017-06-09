# Prism

[![Build Status](https://travis-ci.org/Jah524/prism.svg?branch=master)](https://travis-ci.org/Jah524/prism)

Prism is a neural network library for natural language processing written in pure Clojure.
This library get you a distributed representation of words.

![Visualization with t-sne](https://s3.amazonaws.com/prism-archive/source/embedding.png)

Now prism is ready to work with following models.

- Word2Vec by skip-gram with negative-sampling
- RNNLM with negative-sampling
- Skip-Thought

This library also includes some basic neural network model (e.g. feedforward, RNN(LSTM, GRU), encoder-decoder(LSTM, GRU)).
See demonstration section and [examples](/src/examples) for more detail.

## Usage

`git clone https://github.com/Jah524/prism.git` and `lein repl`.
You can find how to do with prism at [project wiki](https://github.com/Jah524/prism/wiki)

If you want to work on trained model in your project, add following dependency to your `project.clj`,

```
[jah524/prism "0.8.0"]
```

## Demonstration

### Feed Forward

- approximation of sin function with 3 hidden units

```
lein run -m  examples.feedforward.sin3
```

### RNN

you can choose rnn model from #{"standard" "lstm" "gru"} for each example

- with dense input

```
lein run -m examples.rnn.simple-prediction standard
```

- with sparse inputs

```
lein run -m examples.rnn.sparse gru
```

- multi class classification

```
lein run -m examples.rnn.multi-class lstm
```

## License

Copyright © 2017 Jah524

Distributed under the Eclipse Public License either version 1.0

