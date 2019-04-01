# fc4-tool

A tool for reorganizing, restructuring, reformatting, and rendering
[FC4](https://fundingcircle.github.io/fc4-framework/) diagrams.


## Purpose

As explained in [The Toolset](../methodology/toolset.html) section of [the FC4
Methodology](../methodology/):

> This tool was created because when one uses Structurizr Express (SE) to position the elements of a diagram, SE regenerates the diagram source YAML in such a way that the YAML becomes noisy and the sorting can change. This makes the source harder to work with in a text editor and impossible to usefully diff from revision to revision — and without useful diffing it’s very difficult to do effective peer review.
>
> So fc4-tool processes the YAML: cleans it up, applies a stable sort to all properties, removes empty properties, etc — so as to ensure that the changes applied in each revision are very small and specific and all extraneous changes are filtered out. This will hopefully enable effective peer review of revisions to the diagrams.
>
> fc4-tool also:
>
> * “Snaps” the elements and vertices in a diagram to a virtual grid
> * Renders diagrams


## Features

The tool has two main features:

### Formatting

Rewrites [Structurizr Express][structurizr-express] diagram YAML files:

* Removes properties with empty/blank values
* Removes extraneous quotes around values that are obviously strings
* Sorts properties and elements using a known and stable sort order
* “Snaps” the elements and vertices to a virtual grid
  * i.e. rounds their coordinates
* Adjusts element coordinates if necessary so as to create top and left margins in the diagram
  * i.e. ensures that all elements are at least 50px away from the top/left edges of the canvas
* Offsets the positions of `People` elements so they’ll align properly with other kinds of elements
  (working around a rendering quirk of Structurizr Express)


### Rendering

Given [Structurizr Express][structurizr-express] diagram YAML files, creates PNG image files that
contain the visualization of the diagram.

* The resulting image file is created in the same directory as the YAML file, with the same filename
  except its extension is `png`
  * e.g. `docs/spline_reticulator_01_context.yaml` yields `docs/spline_reticulator_01_context.png`
* If the image file already exists it will be overwritten


## Setup

### Requirements

1. A [Java Runtime Environment (JRE)](https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) or [Java Development Kit (JDK)](https://adoptopenjdk.net/)
   1. On MacOS if you have [Homebrew](https://brew.sh/) you can run `brew cask install adoptopenjdk`
1. An installation of [Chrome](https://www.google.com/chrome/browser/) or [Chromium](https://www.chromium.org/Home) **70–72** (inclusive)
   1. On MacOS:
      1. If you have [Homebrew](https://brew.sh/) you can run `brew cask install chromium`
      1. Chromium/Chrome must be at either `/Applications/Chromium.app` or `/Applications/Google Chrome.app`

MacOS quick-start for [Homebrew](https://brew.sh/) users: `brew cask install adoptopenjdk chromium`


### Download and Install

1. Download the archive for your platform from the latest release on [the releases page](https://github.com/FundingCircle/fc4-framework/releases)
1. Expand the archive
1. Optional: move the extracted files to somewhere on your $PATH
   1. e.g. `mv ~/Downloads/fc4/fc4* ~/bin/`


## Authoring Diagrams

### Abridged Workflow

1. Run in your terminal: `fc4 edit path/to/diagram.yaml/or/dir`
1. Open a YAML file in your text editor and edit it
   1. Either a YAML file specified directly or one in or under a specified directory
1. Whenever you save the file, fc4-tool will see the change, clean up the file (overwriting it) and
   render the diagram to a PNG file (overwriting that file, if it already existed)
1. When you’d like to wrap up your session:
   1. Save the file one last time and wait for fc4-tool to process and render it
   1. Hit ctrl-c to exit fc4-tool
   1. Run `git status` and you should see that the YAML file has been created/changed and its
      corresponding PNG file has also been created/changed
   1. Commit both files

### Full Workflow

Please see [The Authoring Workflow](../methodology/authoring_workflow.html) section of
[the FC4 Methodology](../methodology/).


## Commands

The `fc4` program supports multiple commands:

### edit

`fc4 edit file-or-dir ...`

Launches the tool in a persistent mode to effect the authoring workflow described
[above](#abridged-workflow).

* One or more paths must be supplied
* Each Structurizr Express YAML file specified or found in or under specified directories
  (recursively) will be watched for changes
* When a change is observed the tool will [reformat](#formatting) and [render](#rendering) the
  modified file

### format

`fc4 format file ...`

Reformats each specified Structurizr Express YAML file as described [above](#formatting).

* One or more file paths must be supplied
* Does not render

### render

`fc4 render file ...`

Renders each specified Structurizr Express YAML file as described [above](#rendering)

* One or more file paths must be supplied
* The resulting image file is created in the same directory as the YAML file, with the same filename
  except its extension will be `png`
  * e.g. `docs/spline_reticulator_01_context.yaml` yields `docs/spline_reticulator_01_context.png`
* If the image file already exists it will be overwritten
* Does not format or otherwise modify the YAML file

### process

`fc4 process file ...`

[Reformats](#formatting) **and** [renders](#rendering) each specified Structurizr Express YAML file.

* One or more file paths must be supplied


[structurizr-express]: https://structurizr.com/help/express
