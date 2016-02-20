package se.doverfelt;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import se.doverfelt.effects.*;
import se.doverfelt.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class PongzStart extends ApplicationAdapter {
    private static Pool<ParticleEffect> particlePool = Pools.get(ParticleEffect.class);
    long timestamp;
    public static HashMap<String, Entity> entities = new HashMap<String, Entity>();
    private HashMap<String, Effect> effects = new HashMap<String, Effect>();
    SpriteBatch batch;
    BitmapFont font;
    BitmapFont pointFnt;
    public OrthographicCamera camera;
    public float aspect;
    public static int PointsR= 0, PointsL = 0;
    private Texture white;
    private float r = 0, g = 0, b = 0;
    private ArrayList<String> toRemove = new ArrayList<String>();
    private Random rand = new Random();
    private ArrayList<String> toRemoveEffects = new ArrayList<String>();
    private Texture bg;
    private long lastPowerup, timestamp2 = 0;
    public static int Styrning = 2;
    private static boolean effectTextOn;
    private static String effectName;
    private boolean debug = false;
    private int collisionsChecks = 0;
    private int collisions = 0;
    private static ArrayList<ParticleEffect> pEffect = new ArrayList<ParticleEffect>();
    private ArrayList<ParticleEffect> pEffectRemove = new ArrayList<ParticleEffect>();
    public static EffectHandler effectHandler = new EffectHandler();
    public static Pool<EntityPowerup> powerupPool = Pools.get(EntityPowerup.class);
    private static I18NBundle local;
    public static boolean isFlashbanged = false;
    private boolean running = false;
    private float dim = 0;
    SpriteBatch particleBatch;

    @Override
	public void create () {
        aspect = 1f * ((float)Gdx.graphics.getHeight()/(float)Gdx.graphics.getWidth());
        camera = new OrthographicCamera(200f, 200f*aspect);
        camera.position.set(camera.viewportWidth/2f, camera.viewportHeight/2f, 0);
        camera.zoom = 1f;
        camera.update();
        Gdx.input.setCursorCatched(true);
        bg = new Texture("bg.png");
        bg.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        white = new Texture("white.png");
        timestamp = System.currentTimeMillis();
        lastPowerup = System.currentTimeMillis();
        batch = new SpriteBatch();
        particleBatch = new SpriteBatch();
        pointFnt = new BitmapFont(Gdx.files.internal("big.fnt"));
        font = new BitmapFont();
        addEntity(new EntityBall(camera), "ball");
        addEntity(new EntityBorder(0.1f, 0, camera.viewportWidth - 0.2f, 2f), "borderBottom");
        addEntity(new EntityBorder(0.1f, camera.viewportHeight-2f, camera.viewportWidth - 0.2f, 2f), "borderTop");
        addEntity(new EntityPaddle(1, 1, false, this), "paddleLeft");
        addEntity(new EntityPaddle(camera.viewportWidth-3f, 1, true, this), "paddleRight");
        effectHandler.registerEffect(EffectAutoPilot.class);
        effectHandler.registerEffect(EffectDrunk.class);
        effectHandler.registerEffect(EffectRandomColor.class);
        effectHandler.registerEffect(EffectSizeUp.class);
        effectHandler.registerEffect(EffectSpin.class);
        effectHandler.registerEffect(EffectZoomOut.class);
        effectHandler.registerEffect(EffectFlashbang.class);
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        local = I18NBundle.createBundle(Gdx.files.internal("lang"), Locale.getDefault());
        //local = I18NBundle.createBundle(Gdx.files.internal("lang"), new Locale("es", "ES"));

    }

    public void addEntity(Entity entity, String name) {
        entities.put(name, entity);
    }

    public void addPloppable(Ploppable entity, String name, float x, float y) {
        if (entity instanceof EntityPowerup) {
            lastPowerup = System.currentTimeMillis();
        }
        entity.create(name, x, y, this);
        entities.put(name, entity);
    }

    public void removeEntity(String name) {
        toRemove.add(name);
    }

    public void addEffect(Effect effect, String name) {
        effects.put(name, effect);
        effect.create(this, name);
    }

    public void removeEffect(String name) {
        toRemoveEffects.add(name);
    }

    public static void eName(String name){
        effectTextOn = true;
        effectName = local.get(name);

    }
	@Override
	public void render () {
        float delta = (System.currentTimeMillis() - timestamp)/1000f;
        timestamp = System.currentTimeMillis();
        collisionsChecks = 0;
        collisions = 0;

		Gdx.gl.glClearColor(r, g, b, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        /*batch.begin();
        TextureRegion region = new TextureRegion(bg, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
        TextureRegionDrawable drawable = new TextureRegionDrawable(region);
        drawable.draw(batch, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();*/
        if (running) {
            genPowerups();
            tickEffects(delta);
            tickEntities(delta);
            drawEntities();
            drawHUD(delta);
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                running = false;
            }
        } else {
            drawEntities();
            drawHUD(delta);
            drawPause(delta);
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                running = true;
                dim = 0;
            }
        }

        if (!(pEffect.isEmpty())) {
            for (ParticleEffect p : pEffect) {
                if (p.isComplete()) {
                    pEffectRemove.add(p);
                }
                p.scaleEffect(camera.viewportHeight/Gdx.graphics.getHeight());
                p.update(delta);
                particleBatch.setProjectionMatrix(camera.combined);
                particleBatch.begin();
                p.draw(particleBatch, delta);
                particleBatch.end();
            }
        }
       if(effectTextOn){
           if (timestamp2 == 0){ timestamp2 = System.currentTimeMillis();}
           //String n = effectName.substring(effectName.indexOf(' ')+1, effectName.length());
           //effectName = effectName.substring(0, effectName.indexOf(' ')+2);
           batch.begin();
           pointFnt.draw(batch, local.get("powerupMessage"), (Gdx.graphics.getWidth()/2f) - (pointFnt.getSpaceWidth()*(local.get("powerupMessage").length()/2) ), Gdx.graphics.getHeight()/2f + pointFnt.getLineHeight());
           pointFnt.draw(batch, effectName, (Gdx.graphics.getWidth()/2f) - (pointFnt.getSpaceWidth()*(effectName.length()/2f) ), Gdx.graphics.getHeight()/2f );
           //pointFnt.draw(batch, local.format("powerupMessage", effectName), (Gdx.graphics.getWidth()/2f) - (pointFnt.getSpaceWidth()*(local.get("powerupMessage").length()/2)), Gdx.graphics.getHeight()/2f + pointFnt.getLineHeight());
           //pF3.draw(batch, n, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);


           batch.end();
           if( 2000 < System.currentTimeMillis() - timestamp2){
               effectTextOn = false;
               timestamp2 = 0;
           }
       }

        if (Gdx.input.isKeyPressed(Input.Keys.Q)) Gdx.app.exit();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) debug = !debug;

        for (String s : toRemove) {
            if (entities.get(s) instanceof EntityPowerup) {
                powerupPool.free((EntityPowerup)entities.get(s));
            } else {
                entities.get(s).dispose();
            }
            entities.remove(s);
        }
        toRemove.clear();
        for (String toRemoveEffect : toRemoveEffects) {
            effectHandler.free(effects.get(toRemoveEffect).getClass(), effects.get(toRemoveEffect));
            effects.remove(toRemoveEffect);
        }
        toRemoveEffects.clear();

        for (ParticleEffect p : pEffectRemove) {
            particlePool.free(p);
            pEffect.remove(p);
        }
        pEffectRemove.clear();
    }

    private void drawEntities() {
        for (Entity entity : entities.values()) {
            entity.render(camera);
        }
    }

    @Override
    public void pause() {
        running = false;
        Gdx.input.vibrate(500);
    }

    private void drawPause(float delta) {
        dim = Math.min(dim+2*delta, 0.75f);
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0, 0, 0, dim));
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (dim == 0.75f) {
            batch.begin();
            pointFnt.draw(batch, local.get("pause"), Gdx.graphics.getWidth() / 2f - (local.get("pause").length() * pointFnt.getSpaceWidth()) / 2f, Gdx.graphics.getHeight() / 2f - pointFnt.getLineHeight() / 2f);
            batch.end();
        }
    }

    private void genPowerups() {
        if (System.currentTimeMillis() - lastPowerup >= 5000 && rand.nextInt() < 25){
            String n = "powerup"+System.currentTimeMillis();
            addPloppable(powerupPool.obtain(), n, Math.max((rand.nextFloat()*200)-10-3, 3), Math.max((rand.nextFloat()*200*aspect)-10-2, 2));
        }
        if (System.currentTimeMillis()-lastPowerup >= 5000) lastPowerup = System.currentTimeMillis();
    }

    private void tickEffects(float delta) {
        for (Effect effect : effects.values()) {
            effect.update(this, delta);
        }
    }

    private void drawHUD(float delta) {

        String entitiesOut = "";
        for (String s : entities.keySet()) {
            entitiesOut += s + "\n";
        }

        String effectsOut = "";
        for (String s : effects.keySet()) {
            effectsOut += s + "\n";
        }

        batch.begin();
        if (debug) {
            font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond() + "    Delta: " + delta, 1, Gdx.graphics.getHeight() - font.getLineHeight() - 1);
            font.draw(batch, "Entites: " + entities.size() + "\n" + entitiesOut, 1, Gdx.graphics.getHeight() - font.getLineHeight() * 2 - 1);
            font.draw(batch, "Effects: " + effects.size() + "\n" + effectsOut, 1, font.getLineHeight() * (effects.size() + 2));
            font.draw(batch, "CollisionChecks: " + collisionsChecks + "\nCollisions: " + collisions, 1, font.getLineHeight() * (effects.size() + 4));
            font.draw(batch, "Java Heap: " + (Gdx.app.getJavaHeap()/1024/1024) + "MB | Native Heap: " + (Gdx.app.getNativeHeap()/1024/1024) + "MB", 1, Gdx.graphics.getHeight()-(font.getLineHeight()*(entities.size() + 3)));
        }

        String pl = "" + PointsL, pr = "" + PointsR;
        pointFnt.draw(batch, pl, Gdx.graphics.getWidth()/2f - 4 - (pl.length()*pointFnt.getSpaceWidth()), Gdx.graphics.getHeight()-25);
        pointFnt.draw(batch, pr, Gdx.graphics.getWidth()/2f + 4, Gdx.graphics.getHeight()-25);
        batch.draw(white, Gdx.graphics.getWidth()/2f-2, 0, 4f, Gdx.graphics.getHeight());
        batch.end();
        camera.update();
    }

    private void tickEntities(float delta) {
        for (Entity entity : entities.values()) {
            if (entity instanceof Collidable) {
                collisionsChecks++;
                for (Entity c : entities.values()) {
                    if (c instanceof Collidable) {
                        collisionsChecks++;
                        if (Intersector.overlaps(((Collidable) c).getRect(), ((Collidable) entity).getRect()) && entity != c) {
                            collisions++;
                            ((Collidable) entity).collide(c);
                        }
                    }
                }
            }
            entity.update(delta);
        }
    }

    public void setColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
    public float getR(){return this.r;}
    public float getG(){return this.g;}
    public float getB(){return this.b;}

     public static void startParticle(String name, float x, float y, boolean top) {
         ParticleEffect p = particlePool.obtain();
         p.load(Gdx.files.internal(name), Gdx.files.internal("Particles"));
         p.setPosition(x, y);
         if (top) {
             p.flipY();
         }
         pEffect.add(p);
         p.start();
     }

    public HashMap<String, Effect> getEffects() {
        return effects;
    }
}
