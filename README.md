# Sieve Grounder

This repository contains the Scala code for generic grounding.

Grounding means that you have a text you want to ground, together with a list of candidate groundings and the goal is to return the best items from the candidate list (if any). Best can be application dependent, but it typically means the most similar (say, semantically). For example, if we want to ground `"dog"` and the list of candidates is: `["Shiba Inu", "cat", "Brown Bear"]`, then we should (probably) ground it to `"Shiba Inu"` (which is a dog breed).

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

### Sequential Grounder
We allow arbitrary combinations of the aforementioned grounders. Currently, we offer a sequential grounder which is just a list of grounders:
```python
[
    'Exact Matcher', # 1
    'Fuzzy Slop Matcher (slops=[0,1,2,4])', # 2
    'Fuzzy Edit Distance Matcher (max_edit = 0)', # 3
    'Fuzzy Edit Distance Matcher (max_edit = 1)', # 4
    'Fuzzy Edit Distance Matcher (max_edit = 2)', # 5
    'Neural Matcher (threshold = 0.8)', # 6
    'Neural Matcher (threshold = 0.6)', # 7
    'Neural Matcher (threshold = 0.5)', # 8
]
```

As highlighted in the example above, we can have the same component multiple times, with different parameters.
The way the Sequential Grounder will work is to try to ground and successfully return `k` candidates. 
It will first try `Exact Matcher` (component `1`). If this component already returns `k` (or more) candidates, we do not proceed to the next component.
However, if this component only returns `n` candidates, where `n < k`, then we proceed to the next component `Fuzzy Slop Matcher (slops=[0,1,2,4])` (component `2`) and
try to successfully return `k-n` candidates. The same procedure continues until ee get to `k` candidates or we used all the components.

Therefore, it is wise to order these components based on their precision, where the higher precision components come first.

#### How is the functionality of early stopping implemented?

The implementation of early stopping is done using `Stream` in scala:
```scala
components.toStream.flatMap { grounder =>
  grounder.ground(text, context, groundingTargets, k)
}.take(k)
```

Each `grounder` returns a stream. We have a `take(k)` at the end, which ensures that we only to the minimum amount of work necessary to populate the result with `k` items.
This early stopping behavior is specific to `Stream`. Please see the concepts of `lazy collection` if you are interested in reading more.

### Config

We alllow dynamic configuration of the `Sequential Grounder`, using `reference.conf`. Please see an example below.
```
sieve {
  # How many results to return (overridable)
  k = 5
  # Details of each component
  # Contains details such as:
  #   - type (this is from a predefined list)
  #   - fieldNames (over which fields will it operate)
  # 
  component1 {
    name = "Exact Matcher"
    type = "exact_matcher"
    # What fields to search on
    # Order is important; Left -> More important (will be searched first)
    # fieldNames = ["name", "synonym1", "synonym2", "synonym3", "synonym4", "synonym5", "synonym6", "synonym7", "synonym8", "synonym9", "synonym10"]
    fieldNames = [["name", "synonym1", "synonym2", "synonym3", "synonym4", "synonym5", "synonym6", "synonym7", "synonym8", "synonym9", "synonym10"], ]
  }
  component2 {
    name = "Fuzzy Edit Distance Matcher"
    type = "fuzzy_editdistance_matcher"
    # fieldNames = ["name", "synonym1", "synonym2", "synonym3", "synonym4", "synonym5", "synonym6", "synonym7", "synonym8", "synonym9", "synonym10", "description"]
    fieldNames = [["name", "synonym1", "synonym2", "synonym3", "synonym4", "synonym5", "synonym6", "synonym7", "synonym8", "synonym9", "synonym10"], ["description"]]
    editDistance = 2
  }
  component3 {
    name = "Fuzzy Slop Matcher"
    type = "fuzzy_slop_matcher"
    # fieldNames = ["name", "synonym1", "synonym2", "synonym3", "synonym4", "synonym5", "synonym6", "synonym7", "synonym8", "synonym9", "synonym10", "description"]
    fieldNames = [["name", "synonym1", "synonym2", "synonym3", "synonym4", "synonym5", "synonym6", "synonym7", "synonym8", "synonym9", "synonym10"], ["description"]]
    slops = [1, 2, 4, 8]
  }
  component4 {
    name = "Neural Matcher"
    type = "neural_matcher"

    modelPath = "/home/rvacareanu/projects_7_2309/skema_python/results/240128/onnx/model.onnx" # Where to load the model from (overridable)
    threshold = 0.5 # Ground based on score (overridable)
  }
}
```

Please notice that we enumerate each component like so: `component1`, `component2`, etc. Please use consecutive numbers, as when we create the grounder we stop once `component<i>` is not present.

An explanation of these fields is as follows:
- `name` -> The name of the grounder. We use it only to specify additional details once the grounded has ended;
- `type` -> This field specifies which type of grounder is that component. It has to be one of the following:
  - `exact_matcher`
  - `fuzzy_slop_matcher`
  - `fuzzy_editdistance_matcher`
  - `neural_matcher`
- `fieldNames` -> This specifies on which fields from the document to do the searching. Please notice that it is a list of lists `[["name", "synonym1"], ["description"]]`. This is because `["name", "synonym1"]` will have the same priority, which is higher than `["description"]`. In other words, we allow to do early stopping on fields as well, and we consider that a match on `"name"`, for example, is better than a match on `"description"`. Also, please notice the fields `synonym1`, etc. The original documents contain a field called `synonym` which is a list. We index every synonym in a different field. We add `1`, `2`, etc to differentiate between multiple synonyms.

Each component has its own specific fields. For example, a `fuzzy_editdistance_matcher` has `editDistance`. A `fuzzy_slop_matcher` has `slops`, and a `neural_matcher` has `modelPath` and `threshold`. Please notice that a `neural_matcher` does not have `fieldNames` since it operates only over the candidate text.


### Indexing
We index `id`, `name`, `description`, and `synonyms`. Since the document can have multiple `synonyms`, we index them as `synonyms1`, `synonyms2`, etc. 
While this technique for indexing the `synonyms` can be used to artificially differentiate between different synonyms, we do not do this. 
We do not have any way to assess which synonym is better, so we treat each synonym as equally good. If one wants to differentiate, please use `fieldNames` like so `[["name"], ["synonym1"], ["synonym2"], ["description"]]` (i.e., notice that instead of `[["name", "synonym1", "synonym2"], ["description"]]`, we did `[["name"], ["synonym1"], ["synonym2"], ["description"]]`, which means that we first try on `name`, and if we successfully grounded `k`, we stop. If not, we try `synonym1`, etc).

The code for indexing is in `org.clulab.scala_grounders.indexing.BuildIndex`, specifically the method called `buildIndexFromPaths`.

### Grounding Interface

We use a functional-like interface, which looks like this:
```scala
def ground(
  text: String,
  context: Option[String],
  groundingTargets: Seq[DKG],
  k: Int = 1
): Stream[GroundingResultDKG]
```

One peculiarity is that we do not provide an index. The initial implementation has been to index on the fly. While this can be ok for small-scale applications, if one repeteadly grounds over the same targets, it can become unnecessarily slow. To address this while maintaining the same simple interface, each grounder has a function called `mkFast`, which receives an `IndexSearcher` as parameter and returns a `Grounder`. The idea is to allow each grounder to store the index locally. Please see the `Fast*` variants (e.g., `org.clulab.scala_grounders.grounding.FastExactGrounder`).
One can call `mkFast` on an already `Fast` grounder to change the underlying index.

### More information
We provide tests, where one can see how each grounder can be used in isolation together with some expected behavior.
Please see `src/test/scala/org/clulab/scala_grounders`

## Publish
Either local `sbt publishLocal` or on a repository.