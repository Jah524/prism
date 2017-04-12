# Prism

Prism is a handy neural network library for natural language processing written in pure Clojure.
This library get you a distributed representation of words.
Now prism is ready to work with word2vec by skip-gram with negative-sampling.

This library also includes some basic neural network model (e.g. feedforward, LSTM).
See  demonstration section for more detail.

## Usage

Add following dependency to your `project.clj`.

```
fixme, wait a moment
```

### Word2vec

```
(use 'prism.nlp.word2vec)

(make-word2vec your-training-path your-save-path 100 {:workers 4})
;; above exmaple specifies hidden size as 100 and learn your-training-path with 4 workers
;; once learning finished, you can see learned model at your-save-path with .w2v and .em extensions.
;; you can use your learned model or embedding as following steps

(use 'prism.util)
(def model (load-model your-save-path.w2v))
(def em (load-model your-save-path.em))

;; then you can get word embedding
;; note: embeddings are represented as float-array
(word2vec em "word")
(vec (word2vec em "word"))

```

## Demonstration

### Feed Forward

- sin approximation with 3 hidden units

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

## License

Copyright © 2017 Jah524

Distributed under the Eclipse Public License either version 1.0

