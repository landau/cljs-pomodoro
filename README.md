# pOModoro timer

A simple pomodoro timer build with ClojureScript and Om.

[Try it out](http://pomodoro.trevorlandau.net)

## Development

Start the server `lein run`

Build the app `lein cljsbuild auto dev`

Look at pomodoro.cljs and follow steps to start a browser-repl using weasel

## Tests

Well...they are missing :(

## Contributing

Just make a pr. :)

## Changelog

#### 0.0.4
- Move playing sound outside requestAnimationFrame so tab doesn't have to be active

#### 0.0.3
- Automatically cycle between 25/5 sessions

#### 0.0.2
- Minor bug fixes

#### 0.0.1
- Release
