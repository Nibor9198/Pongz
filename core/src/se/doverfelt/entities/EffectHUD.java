package se.doverfelt.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.I18NBundle;
import se.doverfelt.Start;
import se.doverfelt.effects.Effect;
import se.doverfelt.worlds.World;
import se.doverfelt.worlds.WorldPongz;

import java.util.Locale;

/**
 * Created by rickard.doverfelt on 2016-05-25.
 */
public class EffectHUD implements Ploppable {
    private Effect effect;
    float x, y;
    private World world;
    private BitmapFont font;
    private I18NBundle local;
    private String name;

    @Override
    public void create(String name, float x, float y, World world) {
        this.x = x;
        this.y = y;
        this.world = world;
        font = new BitmapFont();
        local = I18NBundle.createBundle(Gdx.files.internal("lang"), new Locale(Start.getPreferences().getString("lang")));
        this.name = name;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
    }

    @Override
    public void render(OrthographicCamera camera, SpriteBatch batch) {
        ShapeRenderer renderer = new ShapeRenderer();
        //renderer.setProjectionMatrix(camera.combined);
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(Color.WHITE);
        renderer.rect(x + font.getSpaceWidth()*local.get(effect.getEffectType()).length() - 2.5f, y - 2.5f, 205f, 25f);
        renderer.setColor(new Color(0, 0.1f, 0, 1));
        renderer.rect(x + font.getSpaceWidth()*local.get(effect.getEffectType()).length(), y, 200, 20);
        renderer.setColor(Color.GREEN);
        renderer.rect(x + font.getSpaceWidth()*local.get(effect.getEffectType()).length(), y, 200*((float)effect.currentTime()/(float)effect.totalTime()), 20);
        renderer.end();

        world.getStart().getFontBatch().begin();
        font.draw(world.getStart().getFontBatch(), local.get(effect.getEffectType()), x + 210 + font.getSpaceWidth()*local.get(effect.getEffectType()).length(), y + 12.5f);
        world.getStart().getFontBatch().end();
    }

    @Override
    public void update(float delta) {
        if (effect.currentTime() >= effect.totalTime()) {
            ((WorldPongz)world).removeEntity(name);
        }
    }

    @Override
    public void dispose() {

    }
}
