#define OPENCV2P4

#include "openfabmap.hpp"
#include <fstream>
#ifdef OPENCV2P4
#include <opencv2/nonfree/nonfree.hpp>
#endif
#include "FabMapCli.h"
#include <stdio.h>
//#include <conio.h>

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>


of2::FabMap *generateFABMAPInstance(cv::FileStorage &settings);
cv::Ptr<cv::FeatureDetector> generateDetector(cv::FileStorage &fs);
cv::Ptr<cv::DescriptorExtractor> generateExtractor(cv::FileStorage &fs);



FabMapCli::FabMapCli(){
}

int FabMapCli::init(std::string settfilename)
{
	//std::string settfilename = "//sdcard//robot//fabmap//settings.yml";
	cv::FileStorage fs;
	fs.open(settfilename, cv::FileStorage::READ);
	if (!fs.isOpened()) {
		std::cerr << "Could not open settings file: " << settfilename <<
			std::endl;
		return -1;

		  
	}
	std::string s = fs["FilePaths"]["TestPath"];

	testvideo=s;
	std::string testPath = fs["FilePaths"]["TestImageDesc"];
	std::string vocabPath = fs["FilePaths"]["Vocabulary"];

	fabmap = generateFABMAPInstance(fs);

	 detector = generateDetector(fs);
	if (!detector) {
		std::cerr << "Feature Detector error" << std::endl;
		return -1;
	}

	 extractor = generateExtractor(fs);
	if (!extractor) {
		std::cerr << "Feature Extractor error" << std::endl;
		return -1;
	}
	
	//load the vocabulary
	std::cout << "Loading Vocabulary" << std::endl;
	fs.open(vocabPath, cv::FileStorage::READ);

		fs["Vocabulary"] >> vocab;
	if (vocab.empty()) {
		std::cerr << vocabPath << ": Vocabulary not found" << std::endl;
		return -1;
	}
	

	//load the test data
	fs.open(testPath, cv::FileStorage::READ);
	
	fs["BOWImageDescs"] >> testImageDescs;
	if (testImageDescs.empty()) {
		std::cerr << testPath << ": Test data not found" << std::endl;
		return -1;
	}

	fs.release();

//	cv::VideoCapture movie;
//	cv::Mat movieframe;
//
//		movie.open(testvideo);
//
//	if (!movie.isOpened()) {
//		std::cerr << ": training movie not found" << std::endl;
//		return 0;
//	}
//
//	while (movie.read(movieframe))
//	{
//		cv::Mat m = cv::Mat();
//		m = movieframe.clone();
//		themovie.push_back(m);
//
//	}
//	movie.release();

	return 1;
}

int FabMapCli::Find(cv::Mat frame)
{
	
	//std::ofstream writer("F:\\fabmap\\results.txt");
	std::vector<of2::IMatch> matches;
	std::vector<of2::IMatch>::iterator l;
	cv::Mat fabmapTestData;
	cv::Mat movieframe,frame2;

	cv::Ptr<cv::DescriptorMatcher> matcher =
		cv::DescriptorMatcher::create("FlannBased");
	cv::BOWImgDescriptorExtractor bide(extractor, matcher);
	bide.setVocabulary(vocab);

	std::ofstream maskw;

		cv::Mat bow;
	std::vector<cv::KeyPoint> kpts;

	
	detector->detect(frame, kpts);
	bide.compute(frame, kpts, bow);
	if (kpts.size() < 4)
		return -1;

	drawKeypoints(frame, kpts, frame2);

	//imshow("test", frame2);
		

   fabmapTestData.push_back(bow);



   fabmap->compare(fabmapTestData, testImageDescs, matches);


 
   
   //fabmap->compare(fabmapTestData,  matches);

   double max = -99999;
   int idx = 0;
   int que = 0;
   double match = 0;
   bool  new_place_max = true; 

   int q = 0;
   for (l = matches.begin(); l != matches.end(); l++) {
	 //  writer << l->likelihood << " " << l->imgIdx << " " << l->queryIdx << " " << l->match;
	  // writer << std::endl;
	   if (l->match > match)
	   {
		 //  if (l->match > matches.front().match) {
			   

			   max = l->likelihood;
			   idx = l->imgIdx;
			   que = l->queryIdx;
			   match = l->match;
		//   }
		   
	   }
	 //  std::cout << l->likelihood << " ";
	   
	  
   }
   
   
   if ((idx >= 0))
   {
	   std::cout << std::endl;
	   std::cout << "num imag " << idx << " match:  " << match << " max:  " << max << " queryIdx:  " << que << std::endl;

	   //std::cout << "num imag " << l->imgIdx << "   " << l->likelihood << std::endl;
	  // writer << "-----------------------------" << std::endl;;


   }
   else
	   std::cout << "NO MATCH" << std::endl;
   // writer.close();

   
   

   return idx;

}

int FabMapCli::GenerateTestData(std::string videoPath, std::string bowImageDescPath)
{
	bool minWords = 0;
	//use a FLANN matcher to generate bag-of-words representations
	cv::Ptr<cv::DescriptorMatcher> matcher =
		cv::DescriptorMatcher::create("FlannBased");
	cv::BOWImgDescriptorExtractor bide(extractor, matcher);
	bide.setVocabulary(vocab);

	//load movie
	cv::VideoCapture movie;
	movie.open(videoPath);

	if (!movie.isOpened()) {
		std::cerr << videoPath << ": movie not found" << std::endl;
		return -1;
	}

	//extract image descriptors
	cv::Mat fabmapTrainData;
	std::cout << "Extracting Bag-of-words Image Descriptors" << std::endl;
	std::cout.setf(std::ios_base::fixed);
	std::cout.precision(0);

	std::ofstream maskw;

	if (minWords) {
		//maskw.open(std::string(bowImageDescPath + "mask.txt").c_str());
	}

	cv::Mat frame, bow;
	std::vector<cv::KeyPoint> kpts;

	while (movie.read(frame)) {
		detector->detect(frame, kpts);
		bide.compute(frame, kpts, bow);

		if (minWords) {
			//writing a mask file
			if (cv::countNonZero(bow) < minWords) {
				//frame masked
				maskw << "0" << std::endl;
			}
			else {
				//frame accepted
				maskw << "1" << std::endl;
				fabmapTrainData.push_back(bow);
			}
		}
		else {
			fabmapTrainData.push_back(bow);
		}

		std::cout << 100.0 * (movie.get(CV_CAP_PROP_POS_FRAMES) /
			movie.get(CV_CAP_PROP_FRAME_COUNT)) << "%    \r";
		fflush(stdout);
	}
	std::cout << "Done                                       " << std::endl;

	movie.release();

	//save training data
	cv::FileStorage fs;
	fs.open(bowImageDescPath, cv::FileStorage::WRITE);
	fs << "BOWImageDescs" << fabmapTrainData;
	fs.release();

	return 0;

}


void FabMapCli::ShowOriginal(int n)
{
	
	//cv::Mat movieframe;


		cv::imshow("original", themovie.at(n));
	

	  cv::waitKey(1);
	
	
}

int FabMapCli::test()
{
	int nImg;
	cv::Mat image;
	cv::namedWindow("test", cv::WINDOW_AUTOSIZE);
	image = cv::imread("C:\\Users\\Jesus\\Pictures\\20150204_182832.jpg", CV_LOAD_IMAGE_COLOR);   // Read the file
	imshow("test", image);
	if (!image.data)                              // Check for invalid input
	{
		std::cout << "Could not open or find the image" << std::endl;
		return -1;
	}
	nImg= Find(image);
	if (nImg >= 0)
		ShowOriginal(nImg);
	system("pause");
	

	image = cv::imread("C:\\Users\\Jesus\\Pictures\\vestidor.jpg", CV_LOAD_IMAGE_COLOR);   // Read the file
	imshow("test", image);
	if (!image.data)                              // Check for invalid input
	{
		std::cout << "Could not open or find the image" << std::endl;
		return -1;
	}
	nImg = Find(image);
	if (nImg >= 0)
		ShowOriginal(nImg);
	system("pause");

	image = cv::imread("C:\\Users\\Jesus\\Pictures\\blanca.png", CV_LOAD_IMAGE_COLOR);   // Read the file
	imshow("test", image);
	cv::waitKey(10);
	cv::waitKey(10);
	if (!image.data)                              // Check for invalid input
	{
		std::cout << "Could not open or find the image" << std::endl;
		return -1;
	}
	nImg = Find(image);
	if (nImg >= 0)
		ShowOriginal(nImg);
	system("pause");

	image = cv::imread("F:\\fabmap\\test\\IMG_20150204_220942.jpg", CV_LOAD_IMAGE_COLOR);   // Read the file
	imshow("test", image);
	if (!image.data)                              // Check for invalid input
	{
		std::cout << "Could not open or find the image" << std::endl;
		return -1;
	}
	nImg = Find(image);
	if (nImg >= 0)
		ShowOriginal(nImg);
	system("pause");


	image = cv::imread("F:\\fabmap\\test\\IMG_20150207_104234.jpg", CV_LOAD_IMAGE_COLOR);   // Read the file
	imshow("test", image);
	if (!image.data)                              // Check for invalid input
	{
		std::cout << "Could not open or find the image" << std::endl;
		return -1;
	}
	nImg = Find(image);
	if (nImg >= 0)
		ShowOriginal(nImg);
	system("pause");


	cv::destroyWindow("test");
	return 0;

}


int FabMapCli::test2()
{

}
