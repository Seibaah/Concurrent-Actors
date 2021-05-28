# Concurrent-Actors

Network building API based on the Actor Model. Designed to allow for speedup for muiltiple threads.

Code has a series generator network. For input 2 1 3 the network outputs 1 2 1 1 2 3.
The network is modeled in the images and is fully reusable.
The program needs a single command line parameter indicating the number of threads. This number has to be between 1 and the number of actors.
Additionally read main to see guidelines on the input and output file guidelines.

See the report for performance and design details.
