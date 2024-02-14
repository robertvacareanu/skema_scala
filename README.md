# Sive Grounder

This repository contains the Scala code for grounding

## Components
The project contains `4` types of grounders:

### Exact Matcher 
The Exact Matcher grounder uses `TermQuery` from Lucene for matching. It looks for exact (i.e., `===`) matches. For example, assuming we search for `"dog"`, and we have (pseudocode): 
```python
Doc1({"field_a": "dog", "field_b": "dog cat"})
Doc2({"field_a": "dog cat", "field_b": "cat dog"})
```

Assuming we search on `field_a` and `field_b`, then `Doc1` is a match (because of `field_a`, `dog` == `dog`)
`Doc2`, however, is not an exact match (`dog` is not == with `dog cat`, `dog` is not == with `cat dog`)

In other words, the field we are searching on should have exactly the text we are searching for, nothing less, nothing more.


### Fuzzy Matcher
To supplement the Exact Matcher and to relax the very strict requirements, we provide two types of Fuzzy Matchers

#### Fuzzy Slop Matcher

This grounder attempts to do `fuzzy matching` with slops (i.e., gaps)
For example, assuming we search for `"dog cat"`, and we have (pseudocode): 
```python
Doc1({"field_a": "dog dog", "field_b": "dog dog cat"})
Doc2({"field_a": "dog cat dog", "field_b": "cat dog"})
```

Assuming we search on `field_a` and `field_b`, then `Doc1` is a match (because of `field_b`),
and `Doc2` is a match (because `field_a`)
This grounder allows for slops (i.e. gaps in between the search phrase). For example: 
- slop=0 means no gap (`dog cat` for `dog cat`),
- slop=1 means a gap of 1 item (`dog _ cat` for `dog cat`)
- slop=2 means a gap of 2 items (`dog _ _ cat` for `dog cat`), etc

One might wonder if a `slop=0` is similar to `Exact Grounder` or not. In other words, one might wonder what happens in a scenario like this:
```python
Doc1({"field_a": "dog dog"})
Doc2({"field_a": "dog dog cat"})
Doc3({"field_a": "cat dog dog"})
Doc4({"field_a": "cat dog dog cat"})
Doc5({"field_a": "cat dog cat dog cat"})
```
Where we try to ground the following text: `dog dog` and we use a slop of 0 (i.e., `slop=0`). 
It is clear that `Doc1` will be grounded, but the question is if (and if yes, which of) `Doc2`, `Doc3`, `Doc4`, or `Doc5` will be grounded.

The answer is that `Doc2`, `Doc3`, and `Doc4` will be grounded, and `Doc5` will not be grounded. 
In other words, a `slop=0` does not behave exactly like an `Exact Grounder`.

### Fuzzy Edit Distance Matcher
This grounder attempts to do `fuzzy matching` with edit distance.
For example, assuming we search for `"dog"`, and we have: 
```python
Doc1({"field_a": "dog"})
Doc2({"field_a": "dogg"})
Doc3({"field_a": "doggg"})
Doc4({"field_a": "cat"})
```

Assuming we search on `field_a` with `max Edit = 1`, then `Doc1` and `Doc2` are a match.
`Doc3` is not because the edit distance between `dog` and `doggg` is larger.
`Doc4` is not because the edit distance between `dog` and `cat` is larger.

Assuming we search on `field_a` with `max Edit = 2`, then `Doc1`, `Doc2`, and `Doc3` are a match.
`Doc4` is not because the edit distance between `dog` and `doggg` is larger.

One might wonder what happens when `max Edit = 0`. Assuming the following example (pseudocode):
```python
Doc1({"field_a": "dog"})
Doc2({"field_a": "cat dog"})
Doc3({"field_a": "dog cat"})
Doc4({"field_a": "cat dog cat"})
```

And we try to ground `dog`, all `Doc1`, `Doc2`, `Doc3`, and `Doc4` will be returned. `Doc2` and `Doc3` will have the same score, and `score(Doc1)` > `score(Doc2)` = `score(Doc3)` > `score(Doc4)`


### Neural Matcher
To go beyond simple lexical match, we provide a Neural Grounder mechanism which is capable (in theory) of more "semantic" matching.

For this, we use an ONNX model which we call from scala. Please make sure to use the same format as the one used during the training phase of the neural network.
The way this grounder works is to obtain a score such as: `neural_network(text, grounding_candidate)`. If this score is greater than a threshold (say, `0.5`), then we return this as an yes (i.e., grounded). Otherwise, we do not return it.