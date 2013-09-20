uniform sampler2D m_Texture;
varying vec2 texCoord;

void main() {
      vec4 texVal = texture2D(m_Texture, texCoord);
      gl_FragColor = texVal;
}

