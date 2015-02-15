
class FabMapCli
{
	of2::FabMap *fabmap;
	cv::Ptr<cv::FeatureDetector> detector;
	cv::Ptr<cv::DescriptorExtractor> extractor;
	cv::Mat vocab;
	cv::Mat testImageDescs;

	std::string testvideo;
	void ShowOriginal(int n);

	cv::vector <cv::Mat> themovie;

public:
	 FabMapCli();
	 int init();
	 int test();
	 int test2();
	 int Find(cv::Mat image);
	 int Find2(cv::Mat image);

};
