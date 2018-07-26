# Change Log

## [0.8.9] - 2018-7-26

### fix

- add clip! to encoder-decoder models to avoid NaN

### change

- changed clipping value from -1 to 1


## [0.8.8] - 2018-7-24

### fix

- skip-thought model skips incomplete conversation line

## [0.8.7] - 2018-4-14

- trivial changes and fixes
- update documentation

## [0.8.6] - 2018-2-26

### update

- dependencies

```
[org.clojure/clojure "1.9.0"]
[net.mikera/core.matrix "0.62.0"]
[clj-time "0.14.2"]
[org.clojure/core.async "0.4.474"]
```


## [0.8.5] - 2018-1-12

### update

- binary-classification-error
  + throws exception wheninvalid expectation given


## [0.8.4] - 2017-10-17

### add

- valid-embedding?

### fix

- merge-param!

## [0.8.0] - 2017-6-9

### add

- attention encoder-decoder
  + GRU
  + LSTM


## [0.7.0] - 2017-6-6

### add

- standard rnn
- Gated Recurrent Unit(GRU)
- encoder-decoder with GRU
- Skip-Thought

### change

- LSTM was moved to src/prism/nn/rnn/
- encoder-decoder-lstm was moved to src/prism/nn/encoder-decoder/

### rename

- sequential-output was renamed to forward (lstm)

## [0.6.0] - 2017-6-1

### add

- encoder-decoder model formally (LSTM only)
- tentatively add batch normalization of feedforward

### change

- trivial changed architecture of each models
- use clojure.core.matrix.random for sampling

### remove

- matrix to be more readable

## [0.5.1] - 2017-5-21

### add

- error-function for multiclass classification
- add LSTM demo for multiclass classification

## [0.5.0] - 2017-5-20
### Changed

- add CHANGELOG.md (this file)
- now prism uses clojure.core.matrix as default matrix implementation
- add orthogonal-initialization for recurrent connection

