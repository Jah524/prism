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
See demonstration section and [examples](/src/examples) for more details.

And prism also provides visualization tool that makes figure like above one.
See [wiki](https://github.com/Jah524/prism/wiki/Visualization).

## Usage

`git clone https://github.com/Jah524/prism.git` and `lein repl`.
You can find how to do with prism at [wiki](https://github.com/Jah524/prism/wiki)

If you want to work on trained model in your project, add following dependency to your `project.clj`,

```
[jah524/prism "0.8.6"]
```

## models for NLP

### Word2vec

```clojure

;make word embeddings
(require '[prism.nlp.word2vec :as w2v])
(def model (w2v/make-word2vec "your-training-path.txt" "model-save-path.w2v" 100 {:workers 2}))

;use word embeddings
(require '[prism.util :as util])
(def em (util/load-model "your-save-path.w2v.em"))
(get em "word") ;=> #vectorz/vector [0.06990837345068174,-0.09570045605373989 ....]

(util/similarity (get em "word1") (get em "word2") true) ;=> 0.03489215427695168

```

- See [Word2vec](https://github.com/Jah524/prism/wiki/Word2Vec) for more details about word2vec

### Skip Thought

``` clojure
(require '[prism.nlp.skip-thought :as st])

(def m (st/make-skip-thought "your-training-path" "your-embedding-path" "model-save-path.st" 200 100 100 :gru {:workers 1 :interval-ms 1000 :ns? false :shared? false}))

(st/skip-thought-vector m ["word1" "word2" "word3"]) ; => #vectorz/vector [0.08737680011079421,0.07707453000181463, ...]

```

## Visualization

Move on to your console.

```
git clone https://github.com/Jah524/prism.git
lein run -m server.handler word2vec `path-to-your-embedding` --port 3003
```
go to `127.0.1.1:3003` on your browser and you can see visuallization tool


![example1](https://s3.amazonaws.com/prism-archive/source/example1.png)

### Items

First, you put together items you want visualize.
Next, give them by text file or input manually.
You can try to use [example.txt](https://s3.amazonaws.com/prism-archive/source/example.txt) (body has took from [clojure.org](https://clojure.org/)).

### t-sne parameters

We use t-sne for dimensionality reduction to visualize.
Perplexity and iteration are needed at least.
The perplexity should not too much bigger or smaller, and iterations also should not be too small.
You can get to know more about parameters in [T-SNE-Java](https://github.com/lejon/T-SNE-Java).

You ready?
Then you just put green button below.

### Result

![example2](https://s3.amazonaws.com/prism-archive/source/visualization-result.png)

You might get result figure top of the page.
Keep in mind we use t-sne(non-deterministic) so that a result is different by trials.
When something doesn't work, check your messages on console.

Note, we use [plotly.js](https://github.com/plotly/plotly.js) for visuallization.



## Basic neural networks

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

