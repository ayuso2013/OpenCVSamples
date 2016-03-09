/*****************************
Copyright 2011 Rafael Muñoz Salinas. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of
      conditions and the following disclaimer.

   2. Redistributions in binary form must reproduce the above copyright notice, this list
      of conditions and the following disclaimer in the documentation and/or other materials
      provided with the distribution.

THIS SOFTWARE IS PROVIDED BY Rafael Muñoz Salinas ''AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Rafael Muñoz Salinas OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those of the
authors and should not be interpreted as representing official policies, either expressed
or implied, of Rafael Muñoz Salinas.
********************************/
#include "marker.h"
#include "CvDrawingUtils.h"
#define _USE_MATH_DEFINES
#include <math.h>
#include <cstdio>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/opencv.hpp>
using namespace cv;
namespace aruco {
/**
 *
 */
Marker::Marker() {
    id = -1;
    ssize = -1;
    Rvec.create(3, 1, CV_32FC1);
    Tvec.create(3, 1, CV_32FC1);
    for (int i = 0; i < 3; i++)
        Tvec.at< float >(i, 0) = Rvec.at< float >(i, 0) = -999999;
}
/**
 *
 */
Marker::Marker(const Marker &M) : std::vector< cv::Point2f >(M) {
    M.Rvec.copyTo(Rvec);
    M.Tvec.copyTo(Tvec);
    id = M.id;
    ssize = M.ssize;
}

/**
 *
*/
Marker::Marker(const std::vector< cv::Point2f > &corners, int _id) : std::vector< cv::Point2f >(corners) {
    id = _id;
    ssize = -1;
    Rvec.create(3, 1, CV_32FC1);
    Tvec.create(3, 1, CV_32FC1);
    for (int i = 0; i < 3; i++)
        Tvec.at< float >(i, 0) = Rvec.at< float >(i, 0) = -999999;
}

/**
 *
*/
////////////////////////////////////////


/**
   * Given a Rotation and a Translation expressed both as a vector, returns the corresponding 4x4 matrix
   */
cv::Mat getRTMatrix ( const cv::Mat &R_,const cv::Mat &T_ ,int forceType=-1  ) {
    cv::Mat M;
    cv::Mat R,T;
    R_.copyTo ( R );
    T_.copyTo ( T );
    if ( R.type() ==CV_64F ) {
        assert ( T.type() ==CV_64F );
        cv::Mat Matrix=cv::Mat::eye ( 4,4,CV_64FC1 );

        cv::Mat R33=cv::Mat ( Matrix,cv::Rect ( 0,0,3,3 ) );
        if ( R.total() ==3 ) {
            cv::Rodrigues ( R,R33 );
        } else if ( R.total() ==9 ) {
            cv::Mat R64;
            R.convertTo ( R64,CV_64F );
            R.copyTo ( R33 );
        }
        for ( int i=0; i<3; i++ )
            Matrix.at<double> ( i,3 ) =T.ptr<double> ( 0 ) [i];
        M=Matrix;
    } else if ( R.depth() ==CV_32F ) {
        cv::Mat Matrix=cv::Mat::eye ( 4,4,CV_32FC1 );
        cv::Mat R33=cv::Mat ( Matrix,cv::Rect ( 0,0,3,3 ) );
        if ( R.total() ==3 ) {
            cv::Rodrigues ( R,R33 );
        } else if ( R.total() ==9 ) {
            cv::Mat R32;
            R.convertTo ( R32,CV_32F );
            R.copyTo ( R33 );
        }

        for ( int i=0; i<3; i++ )
            Matrix.at<float> ( i,3 ) =T.ptr<float> ( 0 ) [i];
        M=Matrix;
    }

    if ( forceType==-1 ) return M;
    else {
        cv::Mat MTyped;
        M.convertTo ( MTyped,forceType );
        return MTyped;
    }
}

void getRTfromMatrix44 ( const cv::Mat &M,  cv::Mat &R,cv::Mat &T ) {

    assert ( M.cols==M.rows && M.cols==4 );
    assert ( M.type() ==CV_32F || M.type() ==CV_64F );
//extract the rotation part
    cv::Mat r33=cv::Mat ( M,cv::Rect ( 0,0,3,3 ) );
    cv::SVD svd ( r33 );
    cv::Mat Rpure=svd.u*svd.vt;
    cv::Rodrigues ( Rpure,R );
    T.create ( 1,3,M.type() );
    if ( M.type() ==CV_32F )
        for ( int i=0; i<3; i++ )
            T.ptr<float> ( 0 ) [i]=M.at<float> ( i,3 );
    else
        for ( int i=0; i<3; i++ )
            T.ptr<double> ( 0 ) [i]=M.at<double> ( i,3 );


}

/////////////////////////////////////////

void Marker::glGetModelViewMatrix(double modelview_matrix[16]) throw(cv::Exception) {
    // check if paremeters are valid
    bool invalid = false;
    for (int i = 0; i < 3 && !invalid; i++) {
        if (Tvec.at< float >(i, 0) != -999999)
            invalid |= false;
        if (Rvec.at< float >(i, 0) != -999999)
            invalid |= false;
    }
    if (invalid)
        throw cv::Exception(9003, "extrinsic parameters are not set", "Marker::getModelViewMatrix", __FILE__, __LINE__);
    Mat Rot(3, 3, CV_32FC1), Jacob;
    Rodrigues(Rvec, Rot, Jacob);

    double para[3][4];
    for (int i = 0; i < 3; i++)
        for (int j = 0; j < 3; j++)
            para[i][j] = Rot.at< float >(i, j);
    // now, add the translation
    para[0][3] = Tvec.at< float >(0, 0);
    para[1][3] = Tvec.at< float >(1, 0);
    para[2][3] = Tvec.at< float >(2, 0);
    double scale = 1;

    modelview_matrix[0 + 0 * 4] = para[0][0];
    // R1C2
    modelview_matrix[0 + 1 * 4] = para[0][1];
    modelview_matrix[0 + 2 * 4] = para[0][2];
    modelview_matrix[0 + 3 * 4] = para[0][3];
    // R2
    modelview_matrix[1 + 0 * 4] = para[1][0];
    modelview_matrix[1 + 1 * 4] = para[1][1];
    modelview_matrix[1 + 2 * 4] = para[1][2];
    modelview_matrix[1 + 3 * 4] = para[1][3];
    // R3
    modelview_matrix[2 + 0 * 4] = -para[2][0];
    modelview_matrix[2 + 1 * 4] = -para[2][1];
    modelview_matrix[2 + 2 * 4] = -para[2][2];
    modelview_matrix[2 + 3 * 4] = -para[2][3];
    modelview_matrix[3 + 0 * 4] = 0.0;
    modelview_matrix[3 + 1 * 4] = 0.0;
    modelview_matrix[3 + 2 * 4] = 0.0;
    modelview_matrix[3 + 3 * 4] = 1.0;
    if (scale != 0.0) {
        modelview_matrix[12] *= scale;
        modelview_matrix[13] *= scale;
        modelview_matrix[14] *= scale;
    }
}



/****
 *
 */
void Marker::OgreGetPoseParameters(double position[3], double orientation[4]) throw(cv::Exception) {

    // check if paremeters are valid
    bool invalid = false;
    for (int i = 0; i < 3 && !invalid; i++) {
        if (Tvec.at< float >(i, 0) != -999999)
            invalid |= false;
        if (Rvec.at< float >(i, 0) != -999999)
            invalid |= false;
    }
    if (invalid)
        throw cv::Exception(9003, "extrinsic parameters are not set", "Marker::getModelViewMatrix", __FILE__, __LINE__);

    // calculate position vector
    position[0] = -Tvec.ptr< float >(0)[0];
    position[1] = -Tvec.ptr< float >(0)[1];
    position[2] = +Tvec.ptr< float >(0)[2];

    // now calculare orientation quaternion
    cv::Mat Rot(3, 3, CV_32FC1);
    cv::Rodrigues(Rvec, Rot);

    // calculate axes for quaternion
    double stAxes[3][3];
    // x axis
    stAxes[0][0] = -Rot.at< float >(0, 0);
    stAxes[0][1] = -Rot.at< float >(1, 0);
    stAxes[0][2] = +Rot.at< float >(2, 0);
    // y axis
    stAxes[1][0] = -Rot.at< float >(0, 1);
    stAxes[1][1] = -Rot.at< float >(1, 1);
    stAxes[1][2] = +Rot.at< float >(2, 1);
    // for z axis, we use cross product
    stAxes[2][0] = stAxes[0][1] * stAxes[1][2] - stAxes[0][2] * stAxes[1][1];
    stAxes[2][1] = -stAxes[0][0] * stAxes[1][2] + stAxes[0][2] * stAxes[1][0];
    stAxes[2][2] = stAxes[0][0] * stAxes[1][1] - stAxes[0][1] * stAxes[1][0];

    // transposed matrix
    double axes[3][3];
    axes[0][0] = stAxes[0][0];
    axes[1][0] = stAxes[0][1];
    axes[2][0] = stAxes[0][2];

    axes[0][1] = stAxes[1][0];
    axes[1][1] = stAxes[1][1];
    axes[2][1] = stAxes[1][2];

    axes[0][2] = stAxes[2][0];
    axes[1][2] = stAxes[2][1];
    axes[2][2] = stAxes[2][2];

    // Algorithm in Ken Shoemake's article in 1987 SIGGRAPH course notes
    // article "Quaternion Calculus and Fast Animation".
    double fTrace = axes[0][0] + axes[1][1] + axes[2][2];
    double fRoot;

    if (fTrace > 0.0) {
        // |w| > 1/2, may as well choose w > 1/2
        fRoot = sqrt(fTrace + 1.0); // 2w
        orientation[0] = 0.5 * fRoot;
        fRoot = 0.5 / fRoot; // 1/(4w)
        orientation[1] = (axes[2][1] - axes[1][2]) * fRoot;
        orientation[2] = (axes[0][2] - axes[2][0]) * fRoot;
        orientation[3] = (axes[1][0] - axes[0][1]) * fRoot;
    } else {
        // |w| <= 1/2
        static unsigned int s_iNext[3] = {1, 2, 0};
        unsigned int i = 0;
        if (axes[1][1] > axes[0][0])
            i = 1;
        if (axes[2][2] > axes[i][i])
            i = 2;
        unsigned int j = s_iNext[i];
        unsigned int k = s_iNext[j];

        fRoot = sqrt(axes[i][i] - axes[j][j] - axes[k][k] + 1.0);
        double *apkQuat[3] = {&orientation[1], &orientation[2], &orientation[3]};
        *apkQuat[i] = 0.5 * fRoot;
        fRoot = 0.5 / fRoot;
        orientation[0] = (axes[k][j] - axes[j][k]) * fRoot;
        *apkQuat[j] = (axes[j][i] + axes[i][j]) * fRoot;
        *apkQuat[k] = (axes[k][i] + axes[i][k]) * fRoot;
    }
}
int Marker::getAngleX(CameraParameters &cp)
{
	float size = ssize ;

	cv::Mat R,T;
	Rvec.copyTo ( R );

    Mat objectPoints(4, 3, CV_32FC1);
    objectPoints.at< float >(0, 0) = 0;
    objectPoints.at< float >(0, 1) = 0;
    objectPoints.at< float >(0, 2) = 0;
    objectPoints.at< float >(1, 0) = size;
    objectPoints.at< float >(1, 1) = 0;
    objectPoints.at< float >(1, 2) = 0;
    objectPoints.at< float >(2, 0) = 0;
    objectPoints.at< float >(2, 1) = size;
    objectPoints.at< float >(2, 2) = 0;
    objectPoints.at< float >(3, 0) = 0;
    objectPoints.at< float >(3, 1) = 0;
    objectPoints.at< float >(3, 2) = size;

    vector< Point2f > imagePoints;
    R.at< float >(0, 2)=0;
    cv::projectPoints(objectPoints, R, Tvec, cp.CameraMatrix, cp.Distorsion, imagePoints);

    //angulo del eje x??
        double ang;
        float x,y;
        x=imagePoints[1].x-imagePoints[0].x;
        y=imagePoints[1].y-imagePoints[0].y;
        ang = 180+atan2(x,y)*180/3.14159;
   return (int) ang;

}
void Marker::draw(Mat &in, Scalar color, int lineWidth, bool writeId) const {
    if (size() != 4)
        return;
    cv::line(in, (*this)[0], (*this)[1], color, lineWidth, CV_AA);
    cv::line(in, (*this)[1], (*this)[2], color, lineWidth, CV_AA);
    cv::line(in, (*this)[2], (*this)[3], color, lineWidth, CV_AA);
    cv::line(in, (*this)[3], (*this)[0], color, lineWidth, CV_AA);
    cv::rectangle(in, (*this)[0] - Point2f(2, 2), (*this)[0] + Point2f(2, 2), Scalar(0, 0, 255, 255), lineWidth, CV_AA);
    cv::rectangle(in, (*this)[1] - Point2f(2, 2), (*this)[1] + Point2f(2, 2), Scalar(0, 255, 0, 255), lineWidth, CV_AA);
    cv::rectangle(in, (*this)[2] - Point2f(2, 2), (*this)[2] + Point2f(2, 2), Scalar(255, 0, 0, 255), lineWidth, CV_AA);
    if (writeId) {
        char cad[100];
        sprintf(cad, "id=%d", id);
        // determine the centroid
        Point cent(0, 0);
        for (int i = 0; i < 4; i++) {
            cent.x += (*this)[i].x;
            cent.y += (*this)[i].y;
        }
        cent.x /= 4.;
        cent.y /= 4.;
        putText(in, cad, cent, FONT_HERSHEY_SIMPLEX, 0.7, color, 2);
        cent.y+=20;
        float f = 180/M_PI;
       // sprintf(cad, "x=%d y = %d, z=%d",(int)(Rvec.at< float >(0, 0)*f),(int) (Rvec.at< float >(1, 0)*f),(int) (Rvec.at< float >(2, 0)*f));
         /*
        cent.y+=20;
         sprintf(cad, "x=%f y = %f",Tvec.at< float >(0, 0),Tvec.at< float >(1, 0));

        putText(in, cad, cent, FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255 - color[0], 255 - color[1], 255 - color[2], 255), 2);

*/


    }
}
void Marker::draw3dAxis(cv::Mat im,CameraParameters &cp)
{

	CvDrawingUtils::draw3dAxis(im,*this,cp);

	char cad[100];


	cv::Mat MarkerToCamera=getRTMatrix( Rvec,Tvec);
	cv::Mat Camera2Marker= MarkerToCamera.inv();
	cv::Mat rv,tv;
	getRTfromMatrix44(Camera2Marker,rv,tv);

	// determine the centroid
	 Point cent(0, 0);
	 for (int i = 0; i < 4; i++) {
	       cent.x += (*this)[i].x;
	       cent.y += (*this)[i].y;
	 }
	 cent.x /= 4.;
	 cent.y /= 4.;
	 cent.y+=20;
	 cent.x-=200;
	 //sprintf(cad, "x= %.1f y= %.1f, z= %.1f",p3f.x*100,p3f.y*100,p3f.z*100);
    // sprintf(cad, "x=%.1f y = %.1f z=%.1f",Tvec.at< float >(0, 0)*100,Tvec.at< float >(1, 0)*100,Tvec.at< float >(2, 0)*100);
	// putText(im, cad, cent, FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255,25,255, 255), 2);
	// cent.y+=20;
	 //sprintf(cad, "x=%.1f y = %.1f z=%.1f",p3f.x*100,p3f.y*100,p3f.z*100);
	 cent.y=50;
	 cent.x=10;
	 sprintf(cad, "x=%.1f y = %.1f z=%.1f",tv.at< float >(0, 0)*100,tv.at< float >(0, 1)*100,tv.at< float >(0, 2)*100);
	 cv::putText(im, cad, cent, FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255,25,255, 255), 2);
	 cent.y+=20;
	 //float f = 180/M_PI;
/*
	 float alfa, beta;
	 alfa = f*atan(Tvec.at< float >(0, 0)/Tvec.at< float >(2, 0));
	 beta = f*atan(Tvec.at< float >(1, 0)/Tvec.at< float >(2, 0));

	 sprintf(cad, "alfa=%.1f beta = %.1f ",alfa,beta);
	 cv::putText(im, cad, cent, FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255,25,255, 255), 2);
*/
	 sprintf(cad, "alfa = %d",getAngleX(cp));
	 cv::putText(im, cad, cent, FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255,25,255, 255), 2);



}

void Marker::getdata(int fill[], CameraParameters &cp)
{

	// id
	// x
	// y
	// z
	// angulo
	// x img
	// y img

	fill[0] = id;

	cv::Mat MarkerToCamera=getRTMatrix( Rvec,Tvec);
	cv::Mat Camera2Marker= MarkerToCamera.inv();
	cv::Mat rv,tv;
	getRTfromMatrix44(Camera2Marker,rv,tv);
	fill[1]= (int) (tv.at< float >(0, 0)*100);
	fill[2]= (int) (tv.at< float >(0, 1)*100);
	fill[3]= (int) (tv.at< float >(0, 2)*100);
	fill[4]= (int) getAngleX(cp);

	 Point cent(0, 0);
	 for (int i = 0; i < 4; i++) {
	      cent.x += (*this)[i].x;
	      cent.y += (*this)[i].y;
	 }
	 fill[5] = (int)(cent.x / 4.);
     fill[6] = (int)(cent.y / 4.);


}

/**
 */
void Marker::calculateExtrinsics(float markerSize, const CameraParameters &CP, bool setYPerpendicular) throw(cv::Exception) {
    if (!CP.isValid())
        throw cv::Exception(9004, "!CP.isValid(): invalid camera parameters. It is not possible to calculate extrinsics", "calculateExtrinsics", __FILE__,
                            __LINE__);
    calculateExtrinsics(markerSize, CP.CameraMatrix, CP.Distorsion, setYPerpendicular);
}

void print(cv::Point3f p, string cad) { cout << cad << " " << p.x << " " << p.y << " " << p.z << endl; }
/**
 */
void Marker::calculateExtrinsics(float markerSizeMeters, cv::Mat camMatrix, cv::Mat distCoeff, bool setYPerpendicular) throw(cv::Exception) {
    if (!isValid())
        throw cv::Exception(9004, "!isValid(): invalid marker. It is not possible to calculate extrinsics", "calculateExtrinsics", __FILE__, __LINE__);
    if (markerSizeMeters <= 0)
        throw cv::Exception(9004, "markerSize<=0: invalid markerSize", "calculateExtrinsics", __FILE__, __LINE__);
    if (camMatrix.rows == 0 || camMatrix.cols == 0)
        throw cv::Exception(9004, "CameraMatrix is empty", "calculateExtrinsics", __FILE__, __LINE__);

    double halfSize = markerSizeMeters / 2.;
    cv::Mat ObjPoints(4, 3, CV_32FC1);
    ObjPoints.at< float >(1, 0) = -halfSize;
    ObjPoints.at< float >(1, 1) = halfSize;
    ObjPoints.at< float >(1, 2) = 0;
    ObjPoints.at< float >(2, 0) = halfSize;
    ObjPoints.at< float >(2, 1) = halfSize;
    ObjPoints.at< float >(2, 2) = 0;
    ObjPoints.at< float >(3, 0) = halfSize;
    ObjPoints.at< float >(3, 1) = -halfSize;
    ObjPoints.at< float >(3, 2) = 0;
    ObjPoints.at< float >(0, 0) = -halfSize;
    ObjPoints.at< float >(0, 1) = -halfSize;
    ObjPoints.at< float >(0, 2) = 0;

    cv::Mat ImagePoints(4, 2, CV_32FC1);

    // Set image points from the marker
    for (int c = 0; c < 4; c++) {
        ImagePoints.at< float >(c, 0) = ((*this)[c].x);
        ImagePoints.at< float >(c, 1) = ((*this)[c].y);
    }

    cv::Mat raux, taux;
    cv::solvePnP(ObjPoints, ImagePoints, camMatrix, distCoeff, raux, taux);
    raux.convertTo(Rvec, CV_32F);
    taux.convertTo(Tvec, CV_32F);
    // rotate the X axis so that Y is perpendicular to the marker plane
    if (setYPerpendicular)
        rotateXAxis(Rvec);
    ssize = markerSizeMeters;
    // cout<<(*this)<<endl;
}


/**
*/

void Marker::rotateXAxis(Mat &rotation) {
    cv::Mat R(3, 3, CV_32F);
    Rodrigues(rotation, R);
    // create a rotation matrix for x axis
    cv::Mat RX = cv::Mat::eye(3, 3, CV_32F);
    float angleRad = M_PI / 2;
    RX.at< float >(1, 1) = cos(angleRad);
    RX.at< float >(1, 2) = -sin(angleRad);
    RX.at< float >(2, 1) = sin(angleRad);
    RX.at< float >(2, 2) = cos(angleRad);
    // now multiply
    R = R * RX;
    // finally, the the rodrigues back
    Rodrigues(R, rotation);
}



/**
 */
cv::Point2f Marker::getCenter() const {
    cv::Point2f cent(0, 0);
    for (size_t i = 0; i < size(); i++) {
        cent.x += (*this)[i].x;
        cent.y += (*this)[i].y;
    }
    cent.x /= float(size());
    cent.y /= float(size());
    return cent;
}

/**
 */
float Marker::getArea() const {
    assert(size() == 4);
    // use the cross products
    cv::Point2f v01 = (*this)[1] - (*this)[0];
    cv::Point2f v03 = (*this)[3] - (*this)[0];
    float area1 = fabs(v01.x * v03.y - v01.y * v03.x);
    cv::Point2f v21 = (*this)[1] - (*this)[2];
    cv::Point2f v23 = (*this)[3] - (*this)[2];
    float area2 = fabs(v21.x * v23.y - v21.y * v23.x);
    return (area2 + area1) / 2.;
}
/**
 */
float Marker::getPerimeter() const {
    assert(size() == 4);
    float sum = 0;
    for (int i = 0; i < 4; i++)
        sum += norm((*this)[i] - (*this)[(i + 1) % 4]);
    return sum;
}
}
