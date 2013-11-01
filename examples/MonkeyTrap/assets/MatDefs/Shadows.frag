#import "Common/ShaderLib/MultiSample.glsllib"

//#define SHOW_BOX
//#define SHOW_DELTA

uniform vec2 g_FrustumNearFar;
uniform vec4 g_ViewPort;

uniform vec4 m_ShadowColor;
uniform COLORTEXTURE m_FrameTexture;
uniform DEPTHTEXTURE m_DepthTexture;

varying vec3 texCoord;
varying vec3 vViewDir;
varying vec3 boxScale;

void main(){
    vec4 color = vec4(1.0);

    vec2 uv = vec2(gl_FragCoord.x/g_ViewPort.z, gl_FragCoord.y/g_ViewPort.w);

    float zBuffer = getDepth( m_DepthTexture, uv ).r;

    //
    // z_buffer_value = a + b / z;
    //
    // Where:
    //  a = zFar / ( zFar - zNear )
    //  b = zFar * zNear / ( zNear - zFar )
    //  z = distance from the eye to the object
    //
    // Which means:
    // zb - a = b / z;
    // z * (zb - a) = b
    // z = b / (zb - a)
    //
    float a = g_FrustumNearFar.y / (g_FrustumNearFar.y - g_FrustumNearFar.x);
    float b = g_FrustumNearFar.y * g_FrustumNearFar.x / (g_FrustumNearFar.x - g_FrustumNearFar.y);
    float z = b / (zBuffer - a);

    float us = b / (gl_FragCoord.z - a);

    float modelScale = 1.0;

    float delta = (z-us) * modelScale;

    #if defined(SHOW_DELTA)
        color = vec4(delta, 0.0, 0.0, 1.0);
    #elif defined(SHOW_BOX)
        color = vec4(texCoord * boxScale,1.0);
    #else

    vec3 view = normalize(vViewDir);
    vec3 scene = texCoord + view * delta;
    vec3 stu = scene * boxScale;

    float xTex = (0.5 - stu.x) * 2.0;
    float zTex = (0.5 - stu.z) * 2.0;
    float t = stu.y;

    float low = (t - 0.75) * 1.33333;
    float hi = (t - 0.75) * 4.0;
    float yTex = low * step(t, 0.75) + hi * step(0.75, t);

    float col = sqrt((xTex * xTex) + (zTex * zTex) + (yTex * yTex));
    float shadow = (1.0 - col);
    color = vec4(m_ShadowColor);
    color.a *= clamp(shadow, 0.0, 0.8);
    #endif

    gl_FragColor = color;
}
