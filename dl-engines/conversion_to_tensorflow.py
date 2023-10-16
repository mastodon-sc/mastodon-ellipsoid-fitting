from stardist.models.model3d import StarDist3D
import os
# remember that tensorflow must be on 1.x (I chose 1.15)
# use python 3.7 as TF won't support later versions (I used 3.6)

if __name__ == '__main__':
    # so stardist can properly load
    os.environ['KMP_DUPLICATE_LIB_OK'] = 'True'
    # load model
    model: StarDist3D = StarDist3D.from_pretrained('3D_demo')
    model.predict_instances()
    model.export_TF(r'D:/IdeaProjects/bioJ/stardist/models/adapted.zip')
