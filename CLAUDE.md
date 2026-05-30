The goal of this project is to provide a rubik's cube Android Application.

* The application is called AndRubik
* The application is limited to 3x3 standard Rubik's Cube
* The application should be delivered as an APK and come with a reproducible build (for future inclusion on f-droid.org)
* The application contains a scrambler, a timer and if possible a solver
* The scrambler must show an interface like https://js.cubing.net/cubing/twisty/ , but in android. The scrambling algorithm can use https://js.cubing.net/cubing/scramble/
* The timer will use standard android backend, BUT it should be designed to allow non-frictional use with a cube. Typically, a cuber touches the android screen and the timer start when the hand *leaves* the screen. a new touch of the screen stops the timer
* The solver should use the android camera to capture the cube state, as in https://ruwix.com/cube-solver/scan/ . The algorithm for solving is available at https://cubing.net/ksolve-js-svg/ and https://cubing.net/ksolve.js/

