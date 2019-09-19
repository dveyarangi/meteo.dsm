package meteo.dsm.vis3d;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class SatelliteCameraController extends GestureDetector {
	
	
	
	/** sphere center point */
	protected Vector3 origin;
	
	protected float radius;
	
	protected float lat, lon;
	
	
	/** The button for rotating the camera. */
	public int ROTATE_BUTTON = Buttons.LEFT;
	/** The angle to rotate when moved the full width or height of the screen. */
	public float rotateAngle = 50f;
	/** The button for translating the camera along the up/right plane */
	public int TRANSLATE_BUTTON = Buttons.RIGHT;
	/** The units to translate the camera when moved the full width or height of the screen. */
	public float translateUnits = 10f; // FIXME auto calculate this based on the target
	/** The button for translating the camera along the direction axis */
	public int FORWARD_BUTTON = Buttons.MIDDLE;
	/** The key which must be pressed to activate rotate, translate and forward or 0 to always activate. */
	public int activateKey = 0;
	/** Indicates if the activateKey is currently being pressed. */
	protected boolean activatePressed;
	/** Whether scrolling requires the activeKey to be pressed (false) or always allow scrolling (true). */
	public boolean alwaysScroll = true;
	/** The weight for each scrolled amount. */
	public float scrollFactor = -0.1f;
	/** World units per screen size */
	public float pinchZoomFactor = 10f;
	/** Whether to update the camera after it has been changed. */
	public boolean autoUpdate = true;
	/** The target to rotate around. */
	public Vector3 target = new Vector3();
	/** Whether to update the target on translation */
	public boolean translateTarget = true;
	/** Whether to update the target on forward */
	public boolean forwardTarget = true;
	/** Whether to update the target on scroll */
	public boolean scrollTarget = false;
	public int FORWARD_KEY = Keys.W;
	protected boolean forwardPressed;
	public int BACKWARD_KEY = Keys.S;
	protected boolean backwardPressed;
	public int ROTATE_RIGHT_KEY = Keys.A;
	protected boolean rotateRightPressed;
	public int ROTATE_LEFT_KEY = Keys.D;
	protected boolean rotateLeftPressed;
	/** The camera. */
	public Camera camera;
	/** The current (first) button being pressed. */
	protected int button = -1;


	private float startX, startY;
	private final Vector3 tmpV1 = new Vector3();
	private final Vector3 tmpV2 = new Vector3();

	protected static class CameraGestureListener extends GestureAdapter {
		public SatelliteCameraController controller;
		private float previousZoom;

		@Override
		public boolean touchDown (float x, float y, int pointer, int button) {
			previousZoom = 0;
			return false;
		}

		@Override
		public boolean tap (float x, float y, int count, int button) {
			return false;
		}

		@Override
		public boolean longPress (float x, float y) {
			return false;
		}

		@Override
		public boolean fling (float velocityX, float velocityY, int button) {
			return false;
		}

		@Override
		public boolean pan (float x, float y, float deltaX, float deltaY) {
			return false;
		}

		@Override
		public boolean zoom (float initialDistance, float distance) {
			float newZoom = distance - initialDistance;
			float amount = newZoom - previousZoom;
			previousZoom = newZoom;
			float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
			return controller.pinchZoom(amount / ((w > h) ? h : w));
		}

		@Override
		public boolean pinch (Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
			return false;
		}
	};

	protected final CameraGestureListener gestureListener;

	protected SatelliteCameraController (final CameraGestureListener gestureListener, final Camera camera, float lat, float lon, float radius) {
		super(gestureListener);
		this.gestureListener = gestureListener;
		this.gestureListener.controller = this;
		this.camera = camera;
		this.lat = lat;
		this.lon = lon;
		this.radius = radius;
		
		origin = new Vector3(0,0,0);
		meteo.util.Vector3 pos = meteo.util.Vector3.GEODETIC(radius, lat, lon);
		camera.position.set(pos.xf(), pos.yf(), pos.zf());
		
		
		meteo.util.Vector3 northerPos = meteo.util.Vector3.GEODETIC(radius, lat+1, lon);
		
		camera.up.set(northerPos.xf(), northerPos.yf(), northerPos.zf()).sub(camera.position);
		camera.lookAt(origin);
		
		camera.update();
	}

	public SatelliteCameraController (final Camera camera, float lat, float lon, float radius) {
		this(new CameraGestureListener(), camera, lat, lon, radius);
	}

	public void update () 
	{
		
		if (rotateRightPressed || rotateLeftPressed || forwardPressed || backwardPressed) {
			final float delta = Gdx.graphics.getDeltaTime();
			if (rotateRightPressed) camera.rotate(camera.up, -delta * rotateAngle);
			if (rotateLeftPressed) camera.rotate(camera.up, delta * rotateAngle);
			if (forwardPressed) {
				camera.translate(tmpV1.set(camera.direction).scl(delta * translateUnits));
				if (forwardTarget) target.add(tmpV1);
			}
			if (backwardPressed) {
				camera.translate(tmpV1.set(camera.direction).scl(-delta * translateUnits));
				if (forwardTarget) target.add(tmpV1);
			}
			if (autoUpdate) camera.update();
		}
	}

	private int touched;
	private boolean multiTouch;

	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		touched |= (1 << pointer);
		multiTouch = !MathUtils.isPowerOfTwo(touched);
		if (multiTouch)
			this.button = -1;
		else if (this.button < 0 && (activateKey == 0 || activatePressed)) {
			startX = screenX;
			startY = screenY;
			this.button = button;
		}
		return super.touchDown(screenX, screenY, pointer, button) || (activateKey == 0 || activatePressed);
	}

	@Override
	public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		touched &= -1 ^ (1 << pointer);
		multiTouch = !MathUtils.isPowerOfTwo(touched);
		if (button == this.button) this.button = -1;
		return super.touchUp(screenX, screenY, pointer, button) || activatePressed;
	}

	protected boolean processButton (float deltaX, float deltaY, int button) {
		if (button == ROTATE_BUTTON) 
		{
			tmpV1.set(origin).sub(camera.position);
			camera.rotateAround(camera.position, tmpV1.nor(), deltaY * rotateAngle);
			
		} 
		else if (button == TRANSLATE_BUTTON) 
		{
			
			lat -= 2*deltaY;
			lon -= 2*deltaX;
			
			meteo.util.Vector3 pos = meteo.util.Vector3.GEODETIC(radius, lat, lon);
			camera.position.set(pos.xf(), pos.yf(), pos.zf());
			camera.lookAt(origin);

		} 
		else if (button == FORWARD_BUTTON) 
		{
			radius += 2*deltaY;
			meteo.util.Vector3 pos = meteo.util.Vector3.GEODETIC(radius, lat, lon);
			camera.position.set(pos.xf(), pos.yf(), pos.zf());
			camera.lookAt(origin);
		}
		if (autoUpdate) camera.update();
		return true;
	}

	@Override
	public boolean touchDragged (int screenX, int screenY, int pointer) {
		boolean result = super.touchDragged(screenX, screenY, pointer);
		if (result || this.button < 0) return result;
		final float deltaX = (screenX - startX) / Gdx.graphics.getWidth();
		final float deltaY = (startY - screenY) / Gdx.graphics.getHeight();
		startX = screenX;
		startY = screenY;
		return processButton(deltaX, deltaY, button);
	}

	@Override
	public boolean scrolled (int amount) {
		return zoom(amount * scrollFactor * translateUnits);
	}

	public boolean zoom (float amount) {
		if (!alwaysScroll && activateKey != 0 && !activatePressed) return false;
		radius -= 5*amount;
		meteo.util.Vector3 pos = meteo.util.Vector3.GEODETIC(radius, lat, lon);
		camera.position.set(pos.xf(), pos.yf(), pos.zf());
		camera.lookAt(origin);
		if (scrollTarget) target.add(tmpV1);
		if (autoUpdate) camera.update();
		return true;
	}

	protected boolean pinchZoom (float amount) {
		return zoom(pinchZoomFactor * amount);
	}

	@Override
	public boolean keyDown (int keycode) {
		if (keycode == activateKey) activatePressed = true;
		if (keycode == FORWARD_KEY)
			forwardPressed = true;
		else if (keycode == BACKWARD_KEY)
			backwardPressed = true;
		else if (keycode == ROTATE_RIGHT_KEY)
			rotateRightPressed = true;
		else if (keycode == ROTATE_LEFT_KEY) rotateLeftPressed = true;
		return false;
	}

	@Override
	public boolean keyUp (int keycode) {
		if (keycode == activateKey) {
			activatePressed = false;
			button = -1;
		}
		if (keycode == FORWARD_KEY)
			forwardPressed = false;
		else if (keycode == BACKWARD_KEY)
			backwardPressed = false;
		else if (keycode == ROTATE_RIGHT_KEY)
			rotateRightPressed = false;
		else if (keycode == ROTATE_LEFT_KEY) rotateLeftPressed = false;
		return false;
	}
}
