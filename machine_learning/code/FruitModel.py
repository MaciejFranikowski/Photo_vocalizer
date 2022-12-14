import tensorflow as tf
import matplotlib.pyplot as plt
import numpy

class FruitModel:
	activationFunction = "relu"
	classNames = ["apple", "banana", "orange"]
	def __init__(self, imageHeight, imageWidth, batchSize):
		self.imageHeight = imageHeight
		self.imageWidth = imageWidth
		self.batchSize = batchSize

	def prepareTrainingDataSet(self,path):
		self.dataSetTrain = self.getDataSet(path, self.batchSize, self.imageHeight, self.imageWidth)

	def prepareValidationgDataSet(self,path):
		self.dataSetValidation = self.getDataSet(path, self.batchSize, self.imageHeight, self.imageWidth)
	
	def prepareTestDataSet(self,path):
		self.dataSetTest = self.getDataSet(path, self.batchSize, self.imageHeight, self.imageWidth)

	def setUpModel(self):
		self.model = tf.keras.Sequential(
			[	
			# first reprocessing ( pixels from 0 to 1, instead of 0-255 )
			# Preprocessing
			# tf.keras.layers.Rescaling(1./255),
			# 32 filters, kernel 3x3, relu function
			# tf.keras.layers.Conv2D(32, 3, activation=self.activationFunction),
			tf.keras.layers.Conv2D(32, 3, activation=self.activationFunction, input_shape=(32,32,3)),
			# downsample the maps by 4, 2x2 -> 1
			tf.keras.layers.MaxPooling2D(),
			# Repeat twice
			tf.keras.layers.Conv2D(32, 3, activation=self.activationFunction),
			tf.keras.layers.MaxPooling2D(),
			tf.keras.layers.Conv2D(32, 3, activation=self.activationFunction),
			tf.keras.layers.MaxPooling2D(),
			# make it one dimensio=nal
			tf.keras.layers.Flatten(),
			# 128 neurons layer
			tf.keras.layers.Dense(128, activation=self.activationFunction),
			# number of classes (3 -> also # last layer neurons)
			tf.keras.layers.Dense(3)
			]
		)
	
	def compileModel(self):
		self.model.compile(
			optimizer="adam",
			# Because we have 3 final layers
			loss = tf.losses.SparseCategoricalCrossentropy(from_logits=True),
			metrics=['accuracy']
		)

	def trainModel(self):
		self.model.fit(
			self.dataSetTrain,
			validation_data = self.dataSetValidation,
			epochs = 10
		)

	def getDataSet(self, path, batchSize, imageHeight, imageWidth):
		return tf.keras.utils.image_dataset_from_directory(
			path,
			image_size = (imageHeight,imageWidth),
			batch_size = batchSize
			)

	def displayImportedTrainingDataset(self):
		plt.figure(figsize=(10,10))
		for images, labels in self.dataSetTrain.take(1):
			for i in range(9):
				ax = plt.subplot(3,3, i +1)
				plt.imshow(images[i].numpy().astype("uint8"))
				plt.title(self.classNames[labels[i]])
				plt.axis("off")
		plt.show()
		action = input()

	###### TESTING

	def displayDatasetClassification(self):
		plt.figure(figsize=(10,10))
		for images, labels in self.dataSetTest.take(1):
			classifications = self.model(images)
			for i in range(9):
				ax = plt.subplot(3,3, i +1)
				plt.imshow(images[i].numpy().astype("uint8"))
				index = numpy.argmax(classifications[i])
				plt.title("Pred: "+ self.classNames[index] + " | Real: " + self.classNames[labels[i]])
				plt.axis("off")
		plt.show()
		action = input()

	def showTextDatasetClassification(self):
		plt.figure(figsize=(10,10))
		for images, labels in self.dataSetTest.take(1):
			classification = self.model(images)
			# array with likleyhood to which class it its assigned
			# its because of the last 3 layers
			print(classification)
	
	def evaluateModel(self):
		# Check how it does on new images
		self.model.evaluate(self.dataSetTest)
	
	# Retrieve a batch of images from the test set
	def getPredictionsApples(self):
		# 47 apples
		# 43 bananas
		newDataSet = tf.keras.utils.image_dataset_from_directory(
			"../data/fruits/test",
			image_size = (self.imageHeight, self.imageWidth),
			batch_size = 47,
			shuffle=False
		)
		image_batch, label_batch = newDataSet.as_numpy_iterator().next()
		predictions = self.model.predict_on_batch(image_batch)
		fileP = newDataSet.file_paths
		for x in range(0,47):
			print(fileP[x], self.classNames[numpy.argmax(predictions[x])])


	def getPredictionsBananas(self):
		# 47 apples
		# 43 bananas
		newDataSet = tf.keras.utils.image_dataset_from_directory(
			"../data/fruits/test",
			image_size = (self.imageHeight, self.imageWidth),
			batch_size = 90,
			shuffle=False
		)
		image_batch, label_batch = newDataSet.as_numpy_iterator().next()
		predictions = self.model.predict_on_batch(image_batch)
		fileP = newDataSet.file_paths
		for x in range(47,90):
			print(fileP[x], self.classNames[numpy.argmax(predictions[x])])

	def getPredictionsOranges(self):
		# 47 apples
		# 43 bananas
		newDataSet = tf.keras.utils.image_dataset_from_directory(
			"../data/fruits/test",
			image_size = (self.imageHeight, self.imageWidth),
			batch_size = 130,
			shuffle=False
		)
		image_batch, label_batch = newDataSet.as_numpy_iterator().next()
		predictions = self.model.predict_on_batch(image_batch)
		fileP = newDataSet.file_paths
		for x in range(90,130):
			print(fileP[x], self.classNames[numpy.argmax(predictions[x])])
		

	##### EXPORT 
	def exporToTfLite(self, fileName):
		converter = tf.lite.TFLiteConverter.from_keras_model(self.model)
		tfLiteModel = converter.convert()
		with open(fileName, "wb") as file:
			file.write(tfLiteModel)
