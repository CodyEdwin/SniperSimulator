package com.snipersim.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Renders the world into an off-screen buffer, then composites it through a
 * lens shader: circular scope mask, edge chromatic aberration, glass tint,
 * vignetting and a subtle heat-shimmer distortion.
 */
public final class ScopeRenderer {
    private static final String VERTEX =
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "varying vec2 v_uv;\n" +
            "void main() {\n" +
            "  v_uv = a_texCoord0;\n" +
            "  gl_Position = u_projTrans * a_position;\n" +
            "}\n";

    private static final String FRAGMENT =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec2 v_uv;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform vec2 u_resolution;\n" +
            "uniform float u_scope;\n" +
            "uniform float u_time;\n" +
            "uniform float u_mirage;\n" +
            "void main() {\n" +
            "  vec2 uv = v_uv;\n" +
            "  float aspect = u_resolution.x / max(1.0, u_resolution.y);\n" +
            "  vec2 p = (uv - 0.5) * vec2(aspect, 1.0);\n" +
            "  float r = length(p);\n" +
            "  float scope = clamp(u_scope, 0.0, 1.0);\n" +
            "  if (scope > 0.002) {\n" +
            "    float radius = 0.464 * scope;\n" +
            "    float edgeMix = smoothstep(radius * 0.42, radius, r);\n" +
            "    float shimmer = sin(p.y * 190.0 + u_time * 2.2) * cos(p.x * 73.0 - u_time * 1.6);\n" +
            "    uv.x += shimmer * 0.0011 * u_mirage * edgeMix;\n" +
            "    uv.y += shimmer * 0.00035 * u_mirage * edgeMix;\n" +
            "    vec2 ca = (uv - 0.5) * 0.0032 * edgeMix;\n" +
            "    float red = texture2D(u_texture, uv + ca).r;\n" +
            "    float green = texture2D(u_texture, uv).g;\n" +
            "    float blue = texture2D(u_texture, uv - ca).b;\n" +
            "    vec3 color = vec3(red, green, blue);\n" +
            "    float rim = smoothstep(radius * 0.66, radius, r);\n" +
            "    color *= 1.0 - rim * 0.38;\n" +
            "    color = mix(color, color * vec3(0.84, 0.94, 1.04), rim * 0.5);\n" +
            "    float mask = 1.0 - smoothstep(radius - 0.0035, radius + 0.0005, r);\n" +
            "    vec3 black = vec3(0.003, 0.005, 0.006);\n" +
            "    vec3 scoped = mix(black, color, mask);\n" +
            "    vec3 base = texture2D(u_texture, v_uv).rgb;\n" +
            "    float reveal = smoothstep(0.02, 0.85, scope);\n" +
            "    gl_FragColor = vec4(mix(base, scoped, reveal), 1.0);\n" +
            "  } else {\n" +
            "    vec3 color = texture2D(u_texture, uv).rgb;\n" +
            "    float vig = smoothstep(0.92, 0.24, r);\n" +
            "    color *= mix(0.80, 1.0, vig);\n" +
            "    gl_FragColor = vec4(color, 1.0);\n" +
            "  }\n" +
            "}\n";

    private FrameBuffer frameBuffer;
    private TextureRegion region;
    private ShaderProgram shader;
    private SpriteBatch batch;
    private int width = 1;
    private int height = 1;
    private float time;
    private boolean shaderReady;

    public ScopeRenderer() {
        batch = new SpriteBatch();
        try {
            shader = new ShaderProgram(VERTEX, FRAGMENT);
            shaderReady = shader.isCompiled();
            if (!shaderReady) {
                Gdx.app.error("Scope", "Scope shader failed: " + shader.getLog());
                shader.dispose();
                shader = null;
            }
        } catch (Exception e) {
            Gdx.app.error("Scope", "Scope shader unavailable", e);
            shader = null;
            shaderReady = false;
        }
    }

    public void resize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        if (frameBuffer != null) {
            frameBuffer.dispose();
        }
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, this.width, this.height, true);
        Texture texture = frameBuffer.getColorBufferTexture();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        region = new TextureRegion(texture);
        region.flip(false, true);
    }

    public void begin() {
        if (frameBuffer == null) {
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        frameBuffer.begin();
        Gdx.gl.glViewport(0, 0, frameBuffer.getWidth(), frameBuffer.getHeight());
    }

    public void end(float scopeAmount, float mirage, float delta) {
        time += delta;
        frameBuffer.end();
        Gdx.gl.glViewport(0, 0, width, height);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        batch.setShader(shader);
        batch.begin();
        if (shader != null) {
            shader.setUniformf("u_resolution", width, height);
            shader.setUniformf("u_scope", scopeAmount);
            shader.setUniformf("u_time", time);
            shader.setUniformf("u_mirage", mirage);
        }
        batch.draw(region, 0, 0, width, height);
        batch.end();
        batch.setShader(null);
    }

    public boolean shaderReady() {
        return shaderReady;
    }

    public void dispose() {
        if (frameBuffer != null) {
            frameBuffer.dispose();
        }
        if (shader != null) {
            shader.dispose();
        }
        batch.dispose();
    }
}
