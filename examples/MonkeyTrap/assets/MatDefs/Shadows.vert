
uniform mat4 g_ViewProjectionMatrix;

attribute vec3 inPosition;  // the world position
attribute vec3 inTexCoord;  // the model space position, relative to a corner
attribute vec3 inTexCoord2; // the x,y,z scale to get from model space to 0->1 space
attribute vec3 inNormal;    // the view direction in model-space

varying vec3 texCoord;
varying vec3 vViewDir;
varying vec3 boxScale;



void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    gl_Position = g_ViewProjectionMatrix * modelSpacePos;

    vViewDir = inNormal;
    texCoord = inTexCoord;
    boxScale = inTexCoord2;
}
