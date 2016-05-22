package org.opencv.samples.tutorial2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.video.Video;










//import es.ava.aruco.CameraParameters;
//import es.ava.aruco.Marker;
//import es.ava.aruco.MarkerDetector;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;

public class Tutorial2Activity extends Activity implements
		CvCameraViewListener2 {
	private static final String TAG = "OCVSample::Activity";

	private static final int VIEW_MODE_RGBA = 0;
	private static final int VIEW_MODE_GRAY = 1;
	private static final int VIEW_MODE_CANNY = 2;
	private static final int VIEW_MODE_FEATURES = 5;
	private static final int VIEW_MODE_CMT = 8;
	private static final int START_TLD = 6;
	private static final int START_CMT = 7;
	private static final int VIEW_MODE_BODY = 9;
	private static final int VIEW_OPTICAL_FLOW = 10;
	private static final int VIEW_OPTICAL_MARKER = 11;
	private static final int VIEW_OPTICAL_DISTANCE = 12;
	
	double[] table;
	long time1;
	double distance=0,Total_distance=0;
	
	static final int WIDTH = 320 ;//240;// 320;
	static final int HEIGHT =240;// 135;// ;//240;0;

	private static final int iGFFTMax = 15;

	private static final float mmarkerSizeMeters =  0.0010f;;

	private int _canvasImgYOffset;
	private int _canvasImgXOffset;

	static boolean uno = true;

	private int mViewMode;
	private Mat mRgba;
	private Mat mIntermediateMat;
	private Mat mGray;

	private MenuItem mItemPreviewRGBA;
	private MenuItem mItemPreviewGray;
	private MenuItem mItemPreviewCanny;
	private MenuItem mItemPreviewFeatures;
	private MenuItem mItemPreviewCMT;
	private MenuItem mItemPreview320;
	private MenuItem mItemPreview640;
	private MenuItem mItemPreview800;
	private MenuItem mItemPreview1024;

	protected ColorBlobDetector mDetector;
	
	private Tutorial3View mOpenCvCameraView;
	SurfaceHolder _holder;

	private Rect _trackedBox = null;
	
	HOGDescriptor mHog;
	
	CascadeClassifier mJavaDetector;
	
	Button buttonBody;
	Button buttonLoad;
	Button buttonSave;
	Button buttonOptFlow;
	Button buttonOptMarker;
	Button buttonPhoto;
	Button buttonMedir;
	
//    public CameraParameters mCamParam;   
//    protected MarkerDetector mDetector;
//    protected Vector<Marker>	 	mDetectedMarkers;
	     
    
	MatOfPoint2f mMOP2fptsPrev;
	Mat matOpFlowThis;
	Mat matOpFlowPrev;	
	Mat mMOP2fptsSafe;

	MatOfPoint2f mMOP2fptsThis;	

	MatOfPoint MOPcorners;
	

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				// Load native library after(!) OpenCV initialization
				//android.os.Debug.waitForDebugger();
				System.loadLibrary("mixed_sample");
				
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.enableFpsMeter();

				mHog=new HOGDescriptor();
				mHog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector()); 
				
				
				
				load_cascade();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};



	public Tutorial2Activity() {
		Log.i(TAG, "Instantiated new " + this.getClass());

	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.tutorial2_surface_view);

		mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial2_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);

		final AtomicReference<Point> trackedBox1stCorner = new AtomicReference<Point>();
		final Paint rectPaint = new Paint();
		rectPaint.setColor(Color.rgb(0, 255, 0));
		rectPaint.setStrokeWidth(5);
		rectPaint.setStyle(Style.STROKE);
		_holder = mOpenCvCameraView.getHolder();
		
		buttonBody=(Button)findViewById(R.id.button1);
		buttonLoad=(Button)findViewById(R.id.button_load);
		buttonSave=(Button)findViewById(R.id.button_save);
		buttonOptFlow= (Button)findViewById(R.id.buttonOptFlow);
		buttonOptMarker= (Button)findViewById(R.id.buttonMarker);
		buttonPhoto= (Button)findViewById(R.id.buttonPhoto);
		buttonMedir= (Button)findViewById(R.id.buttonMedir);
		
		buttonMedir.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				mViewMode=VIEW_OPTICAL_DISTANCE;
				mDetector = new ColorBlobDetector();
				mOpenCvCameraView.setResolution(640, 480);
		    
    		} 
		});
		
		
		buttonPhoto.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				
				mOpenCvCameraView.takePicture(Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/camera/photo"+Long.toString(System.currentTimeMillis())+".bmp");
			}
		});
		
		buttonOptMarker.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
//		        mCamParam = new CameraParameters();	        
//		        mCamParam.readFromXML(Environment.getExternalStorageDirectory().getAbsolutePath()+"/CamParams.xml");
//		        mDetector = new MarkerDetector();	
//		        mDetectedMarkers = new Vector<Marker>();
				
				ArucoInit(Environment.getExternalStorageDirectory().getAbsolutePath()+"/CamParams.xml");
		        mViewMode=VIEW_OPTICAL_MARKER;
			}
		});
		
		buttonOptFlow.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//String path = Environment.getExternalStorageDirectory().toString();
				mMOP2fptsPrev= new MatOfPoint2f();
				matOpFlowThis= new Mat();
				matOpFlowPrev= new Mat();	
				mMOP2fptsSafe= new Mat();;
				MOPcorners = new MatOfPoint();

				mMOP2fptsThis=new MatOfPoint2f();;	

				mViewMode = VIEW_OPTICAL_FLOW;
						    
			    
			    table = new double[480];
			    for (int i = 0;i<480;i++)
			    {
			     double py=i;
			     table[i]=calc_distance( py-320, 45, 740,mOpenCvCameraView.getFocalLength(),270);	    	
			    }

				//CreateTestData(path+"/robot/fabmap/andsettings.yml", path+"/robot/fabmap/entrada.mp4",path+"/robot/fabmap/entrada.yml");

			}
		});
		
		buttonBody.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//String path = Environment.getExternalStorageDirectory().toString();
				
				mViewMode = VIEW_MODE_BODY;
				//CreateTestData(path+"/robot/fabmap/andsettings.yml", path+"/robot/fabmap/entrada.mp4",path+"/robot/fabmap/entrada.yml");

			}
		});
			

		   buttonSave.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					CMTLoad(Environment.getExternalStorageDirectory().getPath()+"/Model.yml");
					uno = false;
					mViewMode = VIEW_MODE_CMT;	
				}
		   });
		   buttonLoad.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						CMTSave(Environment.getExternalStorageDirectory().getPath()+"/Model.yml");
					}


		});
		

		mOpenCvCameraView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// re-init

				final Point corner = new Point(
						event.getX() - _canvasImgXOffset, event.getY()
								- _canvasImgYOffset);
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					trackedBox1stCorner.set(corner);
					Log.i("TAG", "1st corner: " + corner);
					break;
				case MotionEvent.ACTION_UP:
					_trackedBox = new Rect(trackedBox1stCorner.get(), corner);
					if (_trackedBox.area() > 100) {
						Log.i("TAG", "Tracked box DEFINED: " + _trackedBox);
						if (mViewMode == VIEW_MODE_FEATURES)
							mViewMode = START_TLD;
						else
							mViewMode = START_CMT;

					} else
						_trackedBox = null;
					break;
				case MotionEvent.ACTION_MOVE:
					final android.graphics.Rect rect = new android.graphics.Rect(
							(int) trackedBox1stCorner.get().x
									+ _canvasImgXOffset,
							(int) trackedBox1stCorner.get().y
									+ _canvasImgYOffset, (int) corner.x
									+ _canvasImgXOffset, (int) corner.y
									+ _canvasImgYOffset);
					final Canvas canvas = _holder.lockCanvas(rect);
					canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // remove
																				// old
																				// rectangle
					canvas.drawRect(rect, rectPaint);
					_holder.unlockCanvasAndPost(canvas);

					break;
				}

				return true;
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		mItemPreviewRGBA = menu.add("RGBA");
		mItemPreviewGray = menu.add("GRAY");
		mItemPreviewCanny = menu.add("Canny");
		mItemPreviewFeatures = menu.add("TLD");
		mItemPreviewCMT = menu.add("CMT");
		mItemPreview320 = menu.add("320");
		mItemPreview640 = menu.add("640");
		mItemPreview800 = menu.add("800");
		mItemPreview1024 = menu.add("1024");
		
		

	

		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
		mGray = new Mat(height, width, CvType.CV_8UC1);
	}

	public void onCameraViewStopped() {
		mRgba.release();
		mGray.release();
		mIntermediateMat.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		final int viewMode = mViewMode;

		switch (viewMode) {
		case VIEW_MODE_BODY:
		{
			
			 mRgba = inputFrame.rgba();
			 //Mat Gray =Reduce(inputFrame.gray());
			 Mat Gray = inputFrame.gray();
			 
			 
			 MatOfRect bodies = new MatOfRect();
			
			 MatOfDouble weights = new MatOfDouble();
			
		//
			 mHog.detectMultiScale(Gray,bodies,weights); 
			 
			 
			 
			 Rect r=null;
			 Rect [] rects = bodies.toArray();
			 double[] wei=weights.toArray();
			 
			

			double px = (double) mRgba.width() / (double) Gray.width();
			double py = (double) mRgba.height() / (double) Gray.height();
			int imax=-1;
			double amax=0;
			 for (int i=0;i<rects.length;i++)
			{
				r=rects[i];
			
					
					r.x = (int) (r.x * px);
					r.y = (int) (r.y * py);
					r.width = (int) (r.width * px);
					r.height = (int) (r.height * py);
				
				if (wei[i]>amax)
				{
					amax=wei[i];
					imax=i;
				}
				Core.rectangle(mRgba,  r.tl(), r.br(), new Scalar(0, 0, 255,
							0), 5);
			}
			 
			 if (imax>=0)
			 {
				 r=rects[imax];
				 double w = mRgba.width();
			     double h = mRgba.height();
				 double ppx = (w) / (double) (mOpenCvCameraView.getWidth());
			     double ppy = (h) / (double) (mOpenCvCameraView.getHeight());
			     r.x = (int) ((r.x / ppx)+0.1*r.width);
			     r.y = (int) ((r.y / ppy)+r.height*0.1);
				 r.width = (int) (0.8*r.width/ ppx);
				 r.height = (int) (0.8*r.height / ppy);
			     
				 _trackedBox=r;
				 mViewMode=START_CMT;
			 }
//			 else
//			 {
//				 mJavaDetector.detectMultiScale(Gray, bodies);
//				 rects = bodies.toArray();
//				 if (rects.length>0)
//				 {
//					 r=rects[0];
//					 double w = mRgba.width();
//				     double h = mRgba.height();
//					 double ppx = (w) / (double) (mOpenCvCameraView.getWidth());
//				     double ppy = (h) / (double) (mOpenCvCameraView.getHeight());
//				     r.x = (int) ((r.x / ppx)+0.1*r.width);
//				     r.y = (int) ((r.y / ppy)+r.height*0.1);
//					 r.width = (int) (0.8*r.width/ ppx);
//					 r.height = (int) (0.8*r.height / ppy);
//					 
//					 Core.rectangle(mRgba,  r.tl(), r.br(), new Scalar(255, 0, 0,
//								0), 10);
//				     
//					 _trackedBox=r;
//					 mViewMode=START_CMT;
//				 }
//			 }
//			 
		}
			break;
		case VIEW_OPTICAL_DISTANCE:
		{
			 mGray = inputFrame.gray();
			 org.opencv.core.Size s = new Size(3,3);
			 Mat mat=new Mat();
			 Imgproc.GaussianBlur(mGray, mat, s, 2);
			 MinMaxLocResult res = Core.minMaxLoc(mat);
			 
			 mRgba = inputFrame.rgba();
			 
			 /*
			 Mat pointMatRgba = mRgba.submat((int)res.maxLoc.y,(int)res.maxLoc.y+1,(int)res.maxLoc.x,(int)res.maxLoc.x+1);
			 Mat pointMatHsv=new Mat();
			 
			 double[] cols= pointMatRgba.get(0,0);
			 Scalar mBlobColorRgba= new Scalar(cols);
			 
			 
            Imgproc.cvtColor(pointMatRgba,pointMatHsv,Imgproc.COLOR_RGB2HSV);
            Scalar hsv= new Scalar(pointMatHsv.get(0, 0));
			 mDetector.setHsvColor(hsv);
			 mDetector.process(mRgba);
			 List<MatOfPoint> contours = mDetector.getContours();
	         Imgproc.drawContours(mRgba, contours, -1, new Scalar(0,0,255),4);
			 */
			 Core.circle(mRgba, res.maxLoc, 30, new Scalar(0,255,0), 3);
	
			 double distance;
			 double offset = Math.atan(5/320);
			 double alfa = Math.atan((res.maxLoc.x-320.0)/740.0  );
			 distance =63.0/ Math.tan(alfa-offset); 
			 if (mRgba.size().width!=640)
			Core.putText(mRgba, "Select 640x480", new Point(10,50), Core.FONT_HERSHEY_SIMPLEX, 0.7 , new Scalar(255,255,255));
			 else
			 Core.putText(mRgba, "Distance: "+(int)distance+" "+(int)(res.maxLoc.x-320), new Point(10,50), Core.FONT_HERSHEY_SIMPLEX, 0.7 , new Scalar(255,255,255));
			 
			 /*
			 Mat colorLabel = mRgba.submat(4, 68, 4, 68);
	            colorLabel.setTo(mBlobColorRgba);
*/
//	            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
//	            mSpectrum.copyTo(spectrumLabel);
		}
			
			
			break;
		case VIEW_OPTICAL_MARKER:
				{
					mRgba = inputFrame.rgba();
					
				//	detectMarkers(inputFrame.gray().getNativeObjAddr(),mRgba.getNativeObjAddr());
					int pos[] = detectMarker(inputFrame.gray().getNativeObjAddr(),mRgba.getNativeObjAddr());
					
					
					//Mat dst = new Mat();
					//Imgproc.resize(mRgba, dst, new org.opencv.core.Size(800, 480));
					//mRgba = dst;
					if (pos!=null)
					{
						String s="";
						for (int i = 0;i<7;i++)
						{
							s = s+Integer.toString(pos[i])+" ";
						}
						Core.putText(mRgba, s, new Point(10,20), Core.FONT_HERSHEY_SIMPLEX, 0.7 , new Scalar(255,255,255));
					}
						
					
//					mDetector.detect(mRgba, mDetectedMarkers, mCamParam, mmarkerSizeMeters,mRgba);
//					for ( int i=0;i<mDetectedMarkers.size();i++) {
//				           
//						mDetectedMarkers.get(i).draw(mRgba,new Scalar(0,0,255),2,true);
//						mDetectedMarkers.get(i).draw3dAxis(mRgba, mCamParam, new Scalar(0,0,255));
//						
//						Marker marker= mDetectedMarkers.get(i);
//						double d[]= new double[3];
//						marker.getRotation().get(0, 0,d);
//						
//					    					    
//					 Core.putText(mRgba, Double.toString(d[0]), new Point(10,20), Core.FONT_HERSHEY_SIMPLEX, 0.7 , new Scalar(255,255,255));
//					 Core.putText(mRgba, Double.toString(d[1]), new Point(10,60), Core.FONT_HERSHEY_SIMPLEX, 0.7 , new Scalar(255,255,255));
//
//								    
//					 mDetector = new MarkerDetector();	
//					 mDetectedMarkers = new Vector<Marker>();					}
//			
				}
			
			break;
		case VIEW_OPTICAL_FLOW:
				{
					OptFlow(inputFrame.rgba());
				}
			break;
		
		case VIEW_MODE_GRAY:
			// input frame has gray scale format
			Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA,
					4);
			break;
		case VIEW_MODE_RGBA:
			// input frame has RBGA format
			mRgba = inputFrame.rgba();
			break;
		case VIEW_MODE_CANNY:
			// input frame has gray scale format
			mRgba = inputFrame.rgba();
			Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80, 100);
			Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA,
					4);
			break;
		case START_TLD: {
			
			mRgba = inputFrame.rgba();
			mGray = Reduce(inputFrame.gray());
			double w = mGray.width();
			double h = mGray.height();
			if (_trackedBox == null)
				OpenTLD(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(),
						(long) (w / 2 - w / 4), (long) (h / 2 - h / 4),
						(long) w / 2, (long) h / 2);
			else {

				Log.i("TAG", "START DEFINED: " + _trackedBox.x / 2 + " "
						+ _trackedBox.y / 2 + " " + _trackedBox.width / 2 + " "
						+ _trackedBox.height / 2);

				double px = (w) / (double) (mOpenCvCameraView.getWidth());
				double py = (h) / (double) (mOpenCvCameraView.getHeight());
				//
				OpenTLD(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(),
						(long) (_trackedBox.x * px),
						(long) (_trackedBox.y * py),
						(long) (_trackedBox.width * px),
						(long) (_trackedBox.height * py));
			}
			uno = false;
			mViewMode = VIEW_MODE_FEATURES;
		}
			break;
		case START_CMT: {
			mRgba = inputFrame.rgba();
			mGray = Reduce(inputFrame.gray());
			double w = mGray.width();
			double h = mGray.height();
			if (_trackedBox == null)
				// OpenTLD(mGray.getNativeObjAddr(),
				// mRgba.getNativeObjAddr(),(long)(w/2-w/4),(long)(
				// h/2-h/4),(long)w/2,(long)h/2);
				OpenCMT(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(),
						(long) (w / 2 - w / 4), (long) (h / 2 - h / 4),
						(long) w / 2, (long) h / 2);
			else {

				Log.i("TAG", "START DEFINED: " + _trackedBox.x / 2 + " "
						+ _trackedBox.y / 2 + " " + _trackedBox.width / 2 + " "
						+ _trackedBox.height / 2);

				double px = (w) / (double) (mOpenCvCameraView.getWidth());
				double py = (h) / (double) (mOpenCvCameraView.getHeight());
				//
				OpenCMT(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(),
						(long) (_trackedBox.x * px),
						(long) (_trackedBox.y * py),
						(long) (_trackedBox.width * px),
						(long) (_trackedBox.height * py));
			}
			uno = false;
			mViewMode = VIEW_MODE_CMT;
		}
			break;

		case VIEW_MODE_FEATURES:
			// input frame has RGBA format
			mRgba = inputFrame.rgba();
			mGray = inputFrame.gray();
			mGray = Reduce(mGray);

			Mat mRgba2 = ReduceColor(mRgba);

			// FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
			if (uno) {
				int w = mGray.width();
				int h = mGray.height();
				OpenTLD(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(),
						(long) w - w / 4, (long) h / 2 - h / 4, (long) w / 2,
						(long) h / 2);
				uno = false;
			} else {

				ProcessTLD(mGray.getNativeObjAddr(), mRgba2.getNativeObjAddr());
				double px = (double) mRgba.width() / (double) mRgba2.width();
				double py = (double) mRgba.height() / (double) mRgba2.height();
				int[] l = getRect();
				if (l != null) {
					Rect r = new Rect();
					r.x = (int) (l[0] * px);
					r.y = (int) (l[1] * py);
					r.width = (int) (l[2] * px);
					r.height = (int) (l[3] * py);

					Core.rectangle(mRgba, r.tl(), r.br(), new Scalar(0, 0, 255,
							0), 5);
				}
				uno = false;

				// Mat mTemp=mRgba;

				// mRgba=UnReduceColor(mRgba2,mTemp.width(),mTemp.height());
				// mTemp.release();

			}

			// mRgba2.release();
			// mGray.release();
			break;

		case VIEW_MODE_CMT:
		// input frame has RGBA format
		{
			mRgba = inputFrame.rgba();
			mGray = inputFrame.gray();
			mGray = Reduce(mGray);

			mRgba2 = ReduceColor(mRgba);

			// FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
			if (uno) {
				int w = mGray.width();
				int h = mGray.height();
				OpenCMT(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(),
						(long) w - w / 4, (long) h / 2 - h / 4, (long) w / 2,
						(long) h / 2);
				uno = false;
			} else {

				ProcessCMT(mGray.getNativeObjAddr(), mRgba2.getNativeObjAddr());
				double px = (double) mRgba.width() / (double) mRgba2.width();
				double py = (double) mRgba.height() / (double) mRgba2.height();

				int[] l = CMTgetRect();
				if (l != null) {
					Point topLeft = new Point(l[0] * px, l[1] * py);
					Point topRight = new Point(l[2] * px, l[3] * py);
					Point bottomLeft = new Point(l[4] * px, l[5] * py);
					Point bottomRight = new Point(l[6] * px, l[7] * py);

					Core.line(mRgba, topLeft, topRight, new Scalar(255, 255,
							255), 3);
					Core.line(mRgba, topRight, bottomRight, new Scalar(255,
							255, 255), 3);
					Core.line(mRgba, bottomRight, bottomLeft, new Scalar(255,
							255, 255), 3);
					Core.line(mRgba, bottomLeft, topLeft, new Scalar(255, 255,
							255), 3);

				}
				uno = false;

				// Mat mTemp=mRgba;

				// mRgba=UnReduceColor(mRgba2,mTemp.width(),mTemp.height());
				// mTemp.release();

			}
		}
			// mRgba2.release();
			// mGray.release();
			break;

		}

		return mRgba;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item == mItemPreviewRGBA) {
			mViewMode = VIEW_MODE_RGBA;
		} else if (item == mItemPreviewGray) {
			mViewMode = VIEW_MODE_GRAY;
		} else if (item == mItemPreviewCanny) {
			mViewMode = VIEW_MODE_CANNY;
		} else if (item == mItemPreviewFeatures) {
			mViewMode = START_TLD;
			_trackedBox = null;
			uno = true;
		} else if (item == mItemPreviewCMT) {
			mViewMode = START_CMT;
			_trackedBox = null;
			uno = true;
		}else if (item == mItemPreview320) {
			//mViewMode = VIEW_MODE_BODY;
			mMOP2fptsPrev= new MatOfPoint2f();
			mOpenCvCameraView.setResolution(320, 240);
		}else if (item == mItemPreview640) {
			mMOP2fptsPrev= new MatOfPoint2f();
			mOpenCvCameraView.setResolution(640, 480); 
				
		}else if (item == mItemPreview800) {
			mMOP2fptsPrev= new MatOfPoint2f();
			mOpenCvCameraView.setResolution(800, 480); 
		
		}else if (item == mItemPreview1024) {
			mMOP2fptsPrev= new MatOfPoint2f();
			mOpenCvCameraView.setResolution(1024, 768); 
		}


		return true;
	}

	private void LoadTLD(String Path) {
		 TLDLoad( Path);
		
		
		
		
	}

	private void SaveTLD(String Path) {
		TLDSave( Path);
		
	}

	Mat Reduce(Mat m) {
		// return m;
		Mat dst = new Mat();
		Imgproc.resize(m, dst, new org.opencv.core.Size(WIDTH, HEIGHT));
		return dst;
	}

	Mat ReduceColor(Mat m) {
		Mat dst = new Mat();
		Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(m, bmp);
		Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);

		Utils.bitmapToMat(bmp2, dst);
		// Imgproc.resize(m, dst, new Size(WIDTH,HEIGHT), 0, 0,
		// Imgproc.INTER_CUBIC);
		return dst;
	}

	Mat UnReduceColor(Mat m, int w, int h) {
		// return m;

		Mat dst = new Mat();
		Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(m, bmp);
		Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, w, h, false);

		Utils.bitmapToMat(bmp2, dst);

		// Imgproc.resize(m, dst, new
		// org.opencv.core.Size(w,h),0,0,Imgproc.INTER_LINEAR);
		m.release();
		return dst;
	}
	
	boolean load_cascade()
	{
		try
		{
		// load cascade file from application resources
        InputStream is = getResources().openRawResource(R.raw.haarcascade_upperbody);
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFile = new File(cascadeDir, "haarcascade_lowerbody.xml");
        FileOutputStream os = new FileOutputStream(mCascadeFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();

        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        
       
       
        if (mJavaDetector.empty()) return false;
		
		} catch (IOException e) {
         e.printStackTrace();
         Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
     }
		return true;
	}

	Mat OptFlow(Mat mRgba)
	{
	
	
	
	if (mMOP2fptsPrev.rows() == 0) {

        //Log.d("Baz", "First time opflow");
        // first time through the loop so we need prev and this mats
        // plus prev points
        // get this mat
        Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

        // copy that to prev mat
        matOpFlowThis.copyTo(matOpFlowPrev);

        // get prev corners
        Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, iGFFTMax, 0.05, 20);
        mMOP2fptsPrev.fromArray(MOPcorners.toArray());

        // get safe copy of this corners
        mMOP2fptsPrev.copyTo(mMOP2fptsSafe);
        
        time1 = System.currentTimeMillis();
        }
    else
        {
        //Log.d("Baz", "Opflow");
        // we've been through before so
        // this mat is valid. Copy it to prev mat
        matOpFlowThis.copyTo(matOpFlowPrev);

        // get this mat
        Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

        // get the corners for this mat
        Imgproc.goodFeaturesToTrack(matOpFlowThis, MOPcorners, iGFFTMax, 0.05, 20);
        mMOP2fptsThis.fromArray(MOPcorners.toArray());

        // retrieve the corners from the prev mat
        // (saves calculating them again)
        mMOP2fptsSafe.copyTo(mMOP2fptsPrev);

        // and save this corners for next time through

        mMOP2fptsThis.copyTo(mMOP2fptsSafe);
        }


    MatOfByte mMOBStatus= new MatOfByte();
	MatOfFloat mMOFerr= new MatOfFloat();
	/*
    Parameters:
        prevImg first 8-bit input image
        nextImg second input image
        prevPts vector of 2D points for which the flow needs to be found; point coordinates must be single-precision floating-point numbers.
        nextPts output vector of 2D points (with single-precision floating-point coordinates) containing the calculated new positions of input features in the second image; when OPTFLOW_USE_INITIAL_FLOW flag is passed, the vector must have the same size as in the input.
        status output status vector (of unsigned chars); each element of the vector is set to 1 if the flow for the corresponding features has been found, otherwise, it is set to 0.
        err output vector of errors; each element of the vector is set to an error for the corresponding feature, type of the error measure can be set in flags parameter; if the flow wasn't found then the error is not defined (use the status parameter to find such cases).
    */
    Video.calcOpticalFlowPyrLK(matOpFlowPrev, matOpFlowThis, mMOP2fptsPrev, mMOP2fptsThis, mMOBStatus, mMOFerr);

    List<Point> cornersPrev = mMOP2fptsPrev.toList();
    List<Point> cornersThis = mMOP2fptsThis.toList();
    List<Byte> byteStatus = mMOBStatus.toList();

    int y = byteStatus.size() - 1;
    int x;
    distance=0;
    for (x = 0; x < y; x++) {
        if (byteStatus.get(x) == 1) {
            Point pt = cornersThis.get(x);
            Point pt2 = cornersPrev.get(x);
            
            Core.circle(mRgba, pt, 5, new Scalar(255,0,0                                                                                                                                                                                                                                                                                                                    ), 3);

            Core.line(mRgba, pt, pt2, new Scalar(0,0,255), 2);
            if (inrange((int)pt.y,(int)pt2.y))
            		distance=distance+table[(int)pt.y]-table[(int)pt2.y];
            }
        }
    if (y>0)
    	distance = distance/y;
    Total_distance+=distance;
    int d =(int) Total_distance;
    long t2= System.currentTimeMillis();
    
    double vel = (distance *1000)/ (t2-time1); //cm/seg
    Core.putText(mRgba, " "+(int)vel+" "+d, new Point(10,40), Core.FONT_HERSHEY_SIMPLEX, 1 , new Scalar(255,255,255));
   // double velo = ;
    time1=t2;
    return mRgba;
	}

	boolean inrange(int a,int b)
	{
		if (a<480)
			if (a>-1)
				if (b<480)
					if (b>-1)
						return true;
						
				
		return false;
		
	}
	
    double calc_distance(double py, double alfa, double fy,double Fy,double h2)
    /*alfa = inclinación cámara
     * py= pixel y
     * fy=distancia focal y en pixels
     * Fy=distancia focal y en milimetros
     * h2= altura de la cámara en mm
     * d= distancia al punto en mm
     */
    {
    	double PI = 3.14159;
    	double falfa= (alfa/180)*PI;
    	double h1= Fy * Math.cos(falfa);
    	double beta =Math.atan(py/fy);
    	double delta = PI/2-falfa-beta;
    	double H=h1+h2;
    	double D=H/Math.tan(delta);
    	double L = Fy*Math.sin(falfa);
    	double d = D-L;
    	return d;
    }
	
	
	public native void FindFeatures(long matAddrGr, long matAddrRgba);

	public native void OpenTLD(long matAddrGr, long matAddrRgba, long x,
			long y, long w, long h);

	public native void ProcessTLD(long matAddrGr, long matAddrRgba);

	private static native int[] getRect();

	public native void OpenCMT(long matAddrGr, long matAddrRgba, long x,
			long y, long w, long h);

	public native void ProcessCMT(long matAddrGr, long matAddrRgba);
	
	public native void TLDLoad(String Path);
	
	public native void TLDSave(java.lang.String Path);
	
	public native void CMTSave(java.lang.String Path);
	public native void CMTLoad(java.lang.String Path);
	
	public native void ArucoInit(java.lang.String Path);
	public native void detectMarkers(long matAddrGray, long matAddrRgba);
	public static native int[] detectMarker(long matAddrGray, long matAddrRgba);

	private static native int[] CMTgetRect();
	private static native void CreateTestData(java.lang.String PathSettings,java.lang.String PathVideo,java.lang.String PathData);

}
