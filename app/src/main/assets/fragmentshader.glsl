#version 300 es

//Input:
in highp vec2 c;

//Output:
layout(location = 0) out lowp vec4 sampleRenderbuffer;

//Uniforms:
//Iterations:
uniform mediump uint iterations;

//Hue texture:
uniform mediump sampler2D hueTexture;

void main()
{
    //Iterate:
    highp vec2 z = c;
    mediump uint i;
    
    for (i = 0u; i < iterations; i++)
    {
        //Condition:
        if (dot(z, z) > 4.0)
            break;
        
        //Step:
        z = vec2((z.x * z.x) - (z.y * z.y), 2.0 * z.x * z.y) + c;
    }
    
    //Get a relative, smooth hue value:
    mediump float hue = float(i) / float(iterations);
    
    //Do a texture lookup:
    sampleRenderbuffer = texture(hueTexture, vec2(hue, 0.5));
}
