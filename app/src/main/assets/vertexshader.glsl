#version 300 es

//Input:
//Vertex data:
layout(location = 0) in vec4 position;

//Output:
//The value to sample:
out vec2 c;

//Uniforms:
//Half frame (Gaussian):
uniform vec2 gaussianHalfFrame;

//Position (Gaussian):
uniform vec2 gaussianPosition;

void main()
{
    //Set the current position (this is always (-1 | 1)^2):
    gl_Position = position;
    
    //Calculate c:
    c = gaussianPosition + (position.xy * gaussianHalfFrame);
}
