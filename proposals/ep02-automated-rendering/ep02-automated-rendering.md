# FC4 Enhancement Proposal EP02: Automated Rendering

This Enhancement Proposal is:

* By [Avi Flax](https://github.com/aviflax/)
* Part of [a batch of proposals](https://github.com/FundingCircle/fc4-framework/issues/72) being discussed and considered (initially) in October 2018

## Summary

The [current workflow](https://fundingcircle.github.io/fc4-framework/methodology/authoring_workflow.html) for rendering diagram images from their YAML source with fc4-tool is completely manual, onerous, tedious, error-prone, hard to learn, and inefficient. We should replace it with a new workflow enabled by automated image rendering.

## New User Experience (Workflow)

1. The dev runs a command such as:
    1. `fc4 render`
    1. `fc4 render --watch`
    1. `fc4 render views/us/us_system_landscape.yaml`
    1. `fc4 render --watch --model foo/bar/model/ foo/bar/views`
1. fc4-tool would then render the specified view or views into PNG files, using either the specified model path or using the default model path of `./model/`
    1. Every specified view would be rendered to a PNG file in the same directory and with the same file name as the YAML file, except for the extension.
    1. System views would generally be rendered to two PNG files: the System Context diagram and (if any containers are specified) the Container diagram.
    1. If the path to the view(s) was unspecified, the default used would be `./views/`.
         1. If the effective view path (supplied or default) is to a directory, then fc4 will search recursively for view YAML files within that directory.
1. If the `--watch` option is specified, then fc4-tool would stay up and running and watch both the model _and_ the specified view file(s) for changes, and re-render all relevant PNG files when any of the watched files change.

## Desired Implementation Properties

I’m not sure if these are all possible, so let’s think of this as a wish list for now:

* Operates completely locally; does not require any sort of network connectivity
* Has as few additional system requirements as possible, ideally zero
* Operates completely in a single OS process

In other words: ideally a user would install and use fc4-tool as a single simple self-contained program with minimal dependencies and it’d just do its job on its own quickly and simply with predictable, straightforward failure modes, low resource utilization, and no race conditions.

## Possible Implementation Approaches

I don’t actually know yet how this will/should work. But! I have some ideas:

### Structurizr’s Web API

Structurizr does have a Web API, and I’ve [corresponded](https://gist.github.com/aviflax/e274b87e558b3ca3d24a8c3f81843fc5) with Simon Brown about whether and how we might be able to use it to programmatically render diagram images. It seems doable.

#### Cons

* Would depend on Structurizr being reachable and “up”
* Would _probably_ depend on the user having access to a paid Structurizr account or instance. (My impression is that the free plan wouldn’t support this.) (OTOH, we might be able to do this with the free plan by uploading the “workspace” as public, downloading the PNG(s), and then deleting the workspace. But that might be slow and/or error prone and/or have unacceptable failure modes.)

### Structurizr Express + Scripted Headless Browser

I had already started playing around with this approach when Simon Brown suggested it in [our correspondence](https://gist.github.com/aviflax/e274b87e558b3ca3d24a8c3f81843fc5). Using [Puppeteer](https://pptr.dev) with headless Chrome this _should_ be fairly straightfoward. (I didn’t quite get it to work when I was playing around with it, but I didn’t spend much time on it back then; I didn’t have a chance to see it through.)

#### Cons

* Would depend on Structurizr Express being reachable and “up”
* Would add lots of system dependencies to fc4-tool: Node, Chrome, and maybe ClojureScript. (Unless we maybe want to consider rigging up a Web service that’d do the rendering by running all that on the server, which’d still be a significant cost+downside.)

### Whimsical’s Hypothetical Web API

[Whimsical](https://whimsical.co/) is a commercial, closed-source, proprietary Web application for GUI-based diagramming. It has a compelling UX and produces very sharp diagrams that are aesthetically attractive. Based on reading blog posts by its creator, I gathered that it’s a data-driven application, so I [inquired](https://gist.github.com/aviflax/0f22c7b79dd76ecbaa9e19fe1bd652a0) as to whether they’d be interested in potentially providing a commercial Web service that would accept data and return images. The outcome of our discussion back in March of this year (2018) was that they definitely wanted to offer a full Web API at some point, not at that point but possible in ~6 months. We might want to consider getting back in touch to see whether this might be a viable scenario at this point.

### Paper.js + Scripted Headless Browser

I saw in [Whimsical’s architecture diagram](https://whimsical.co/Q5patpyGV3RDvkvkx1f6Ck) that their graphics are rendered with [Paper.js](http://paperjs.org/). So I figure if they can use it to render attractive, clear, sharp vector graphics that can be exported as bitmaps, then maybe we can too. Would probably have to run in a headless browser, but might be acceptable.

#### Cons

* Pretty low-level; potentially lots of work to get the layout and aesthetics right
* Would add lots of system dependencies to fc4-tool: Node, Chrome, and maybe ClojureScript.

### Paper.js + JSDom

I found [in Paper’s README](https://github.com/paperjs/paper.js#installing-paperjs-for-nodejs) that it apparently supports rendering graphics in Node, as opposed to in a browser, via [jsdom](https://github.com/jsdom/jsdom). This might be desirable because it wouldn’t require the user to have a recent version of Chrome installed. And, _if_ we were to [migrate fc4-tool to ClojureScript](#on-clojurescript), then this approach might not require any additional system dependencies beyond Node.

#### Cons

* Pretty low-level; potentially lots of work to get the layout and aesthetics right
* Might add a system dependency to fc4-tool: Node — unless we [migrate fc4-tool to ClojureScript](#on-clojurescript)

### JGraphX

[JGraphX](https://github.com/jgraph/jgraphx) is a [Java](https://en.wikipedia.org/wiki/Java_%28programming_language%29) [Swing](https://en.wikipedia.org/wiki/Swing_%28Java%29) diagramming (graph visualisation) library. Back in February I created [a branch of fc4-tool](https://github.com/FundingCircle/fc4-framework/tree/jgraph) (and  [today](https://github.com/FundingCircle/fc4-framework/commit/41b646da8c54b067fd904b7e3cb27f6ed6d9347c#diff-79bf787a4b989cb5ae4e7b2571ce746c) I rebased it onto master and adapted it to the current state of the tool) that contains an experimental implementation of rendering a Structurizr Express diagram to a Swing view or [to a PNG file](https://github.com/FundingCircle/fc4-framework/blob/jgraph/tool/examples/internet_banking_context.png). It’s somewhat promising at the time; it can already render the diagrams such that they are recognizable and legible. But they aren't currently aesthetically attractive, and I don’t really have a clue whether it’s possible and/or practical to make Swing graphics aesthetically attractive.

### Apache Batik

[Batik](https://xmlgraphics.apache.org/batik/) is:

> a Java-based toolkit for applications or applets that want to use images in the Scalable Vector Graphics (SVG) format for various purposes, such as display, generation or manipulation.

…so I’d guess we’d be able to use it to generate “native” vector graphics and export them as PDF and PNG. The most compelling advantage here would be the ability to render images without any external dependencies such as Node, Puppeteer, Chrome, etc.

### Quil

[Quil](http://quil.info/) is a Clojure/ClojureScript library for creating interactive drawings and animations. It’s based on [Processing](https://processing.org/). I don’t remember exactly why, but at some point I thought it might be worth trying to use it to implement diagram rendering “from scratch” i.e. at a very low level. I have an old branch somewhere with a single commit in it, but really there’s barely anything in there. It’s really just a stub of an idea.

### Summary

<table>
  <thead>
  	<tr>
  		<th>Approach</th>
  		<th>Few System Dependencies</th>
  		<th>Single Process</th>
  		<th>Works offline</th>
  	</tr>
  </thead>
  <tbody>
  	<tr>
  		<th>Structurizr’s Web API</th>
  		<td>✓</td>
  		<td>✓</td>
  		<td>✕</td>
  	</tr>
  	<tr>
  		<th>Structurizr Express + Scripted Headless Browser</th>
  		<td>✕</td>
  		<td>✕</td>
  		<td>✕</td>
  	</tr>
  	<tr>
  		<th>Whimsical’s Hypothetical Web API</th>
  		<td>✓</td>
  		<td>✓</td>
  		<td>✕</td>
  	</tr>
  	<tr>
  		<th>Paper.js + Scripted Headless Browser</th>
  		<td>✕</td>
  		<td>✕</td>
  		<td>✓</td>
  	</tr>
  	<tr>
  		<td><b>Paper.js + JSDom</b><br>
       (*If* we [migrate fc4-tool to ClojureScript](#on-clojurescript))
      </td>
  		<td>✓</td>
  		<td>✓</td>
  		<td>✓</td>
  	</tr>
  	<tr>
  		<th>JGraphX</th>
  		<td>✓</td>
  		<td>✓</td>
  		<td>✓</td>
  	</tr>
  	<tr>
  		<th>Apache Batik</th>
  		<td>✓</td>
  		<td>✓</td>
  		<td>✓</td>
  	</tr>
  	<tr>
  		<th>Quil</th>
  		<td>✓</td>
  		<td>✓?</td>
  		<td>✓?</td>
  	</tr>
  </tbody>
</table>

## On ClojureScript

fc4-tool is currently implemented with “just Clojure” i.e. the O.G. variant of [Clojure](https://clojure.org) that runs on the JVM. That’s as opposed to the other popular variant of Clojure, [ClojureScript](https://clojurescript.org), which is transpiled to JavaScript and thereby runs in browsers and/or [Node.js](https://nodejs.org/). The main reason for this is simply that it’s what I knew, so it was easiest for me. Ease matters sometimes, to some degree, but there are many instances wherein it is outweighed by other priorities, and this might be one of them. In the event that some approach for implementing automated rendering is highly compelling but would require running in Node, I’d be open to considering migrating all of fc4-tool to ClojureScript. (This might yield additional benefits such as reduced execution time.)

## On Other Programming Systems

Give the prior section, wherein I stated that in certain circumstances I’d be open to considering migrating all of fc4-tool to ClojureScript, one might wonder whether I’d be similarly open to migrating it to a completely unrelated programming system,  if we were to become aware of an extremely compelling diagram rendering library that runs in a different platform. My current stance on this is: maybe, but probably not. It’d have to be _extremely_ compelling to drive me to consider it seriously.
