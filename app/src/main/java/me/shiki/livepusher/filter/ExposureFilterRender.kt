package me.shiki.livepusher.filter

/**
 *
 * @author shiki
 * @date 2020/7/9
 *
 */
class ExposureFilterRender(textureId: Int = -1) : BaseFilterRender(textureId) {
    override fun createFragmentSource(): String {
        return """
        precision mediump float;
        varying vec2 ft_Position;
        uniform sampler2D sTexture;
        void main() {
            gl_FragColor = vec4(vec3(1.0 - texture2D(sTexture, ft_Position)), 1.0);
        }
    """
    }
}