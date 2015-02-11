package jme3_ext_pgex;

import com.google.protobuf.ExtensionRegistry
import com.jme3.animation.AnimControl
import com.jme3.animation.Animation
import com.jme3.animation.Bone
import com.jme3.animation.Skeleton
import com.jme3.animation.SkeletonControl
import com.jme3.animation.Track
import com.jme3.asset.AssetManager
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.light.Light
import com.jme3.light.PointLight
import com.jme3.light.SpotLight
import com.jme3.material.MatParam
import com.jme3.material.Material
import com.jme3.material.MaterialDef
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Matrix4f
import com.jme3.math.Quaternion
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import com.jme3.math.Vector4f
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Mesh.Mode
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.VertexBuffer
import com.jme3.scene.VertexBuffer.Type
import com.jme3.shader.VarType
import com.jme3.texture.Image
import com.jme3.texture.Texture
import com.jme3.texture.Texture2D
import com.jme3.util.IntMap
import com.jme3.util.TangentBinormalGenerator
import java.util.HashMap
import java.util.List
import java.util.Map
import jme3_ext_animation.CompositeTrack
import jme3_ext_animation.FloatKeyPoints
import jme3_ext_animation.Identity
import jme3_ext_animation.TrackFactory
import org.slf4j.Logger
import pgex.Datas
import pgex.Datas.Data
import pgex_ext.AnimationsKf
import pgex_ext.AnimationsKf.AnimationKF
import pgex_ext.AnimationsKf.KeyFrame
import pgex_ext.AnimationsKf.TransformKF
import pgex_ext.CustomParams
import pgex_ext.CustomParams.CustomParam
import pgex_ext.CustomParams.CustomParamList

// TODO use a Validation object (like in scala/scalaz) with option to log/dump stacktrace
public class Pgex {
	final AssetManager assetManager;
	final Material defaultMaterial;
	public final ExtensionRegistry registry;

	new(AssetManager assetManager) {
		this.assetManager = assetManager
		registry = setupExtensionRegistry(ExtensionRegistry.newInstance())
		defaultMaterial = newDefaultMaterial()
	}

	protected def ExtensionRegistry setupExtensionRegistry(ExtensionRegistry r) {
		CustomParams.registerAllExtensions(r)
		AnimationsKf.registerAllExtensions(r)
		r
	}

	protected def Material newDefaultMaterial() {
		val m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
		m.setColor("Color", ColorRGBA.Gray)
		m
	}

	def Vector2f cnv(Datas.Vec2 src, Vector2f dst) {
		dst.set(src.getX(), src.getY());
		return dst;
	}

	def Vector3f cnv(Datas.Vec3 src, Vector3f dst) {
		dst.set(src.getX(), src.getY(), src.getZ());
		return dst;
	}

	def Vector4f cnv(Datas.Vec4 src, Vector4f dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	def Quaternion cnv(Datas.Quaternion src, Quaternion dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	def Vector4f cnv(Datas.Quaternion src, Vector4f dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	def ColorRGBA cnv(Datas.Color src, ColorRGBA dst) {
		dst.set(src.getR(), src.getG(), src.getB(), src.getA());
		return dst;
	}

	def Matrix4f cnv(Datas.Mat4 src, Matrix4f dst) {
		dst.m00 = src.getC00();
		dst.m10 = src.getC10();
		dst.m20 = src.getC20();
		dst.m30 = src.getC30();
		dst.m01 = src.getC01();
		dst.m11 = src.getC11();
		dst.m21 = src.getC21();
		dst.m31 = src.getC31();
		dst.m02 = src.getC02();
		dst.m12 = src.getC12();
		dst.m22 = src.getC22();
		dst.m32 = src.getC32();
		dst.m03 = src.getC03();
		dst.m13 = src.getC13();
		dst.m23 = src.getC23();
		dst.m33 = src.getC33();
		return dst;
	}

	def Mesh.Mode cnv(Datas.Mesh.Primitive v) {
		switch(v) {
		case line_strip: Mode.LineStrip
		case lines: Mode.Lines
		case points: Mode.Points
		case triangle_strip: Mode.TriangleStrip
		case triangles: Mode.Triangles
		default: throw new IllegalArgumentException(String.format("doesn't support %s : %s", v.getClass(), v))
		}
	}

	def VertexBuffer.Type cnv(Datas.VertexArray.Attrib v) {
		switch(v) {
		case position: Type.Position
		case normal: Type.Normal
		case bitangent: Type.Binormal
		case tangent: Type.Tangent
		case color: Type.Color
		case texcoord: Type.TexCoord
		case texcoord2: Type.TexCoord2
		case texcoord3: Type.TexCoord3
		case texcoord4: Type.TexCoord4
		case texcoord5: Type.TexCoord5
		case texcoord6: Type.TexCoord6
		case texcoord7: Type.TexCoord7
		case texcoord8: Type.TexCoord8
		default: throw new IllegalArgumentException(String.format("doesn't support %s : %s", v.getClass(), v))
		}
	}

	//TODO use an optim version: including a patch for no autoboxing : https://code.google.com/p/protobuf/issues/detail?id=464
	def float[] hack_cnv(Datas.FloatBuffer src) {
		val b = newFloatArrayOfSize(src.getValuesCount())
		val l = src.getValuesList()
		for(var i = 0; i < b.length; i++) b.set(i, l.get(i))
		b
	}

	//TODO use an optim version: including a patch for no autoboxing : https://code.google.com/p/protobuf/issues/detail?id=464
	def int[] hack_cnv(Datas.UintBuffer src) {
		val b = newIntArrayOfSize(src.getValuesCount())
		val l = src.getValuesList();
		for(var i = 0; i < b.length; i++) b.set(i, l.get(i))
		b
	}

	def Mesh cnv(Datas.Mesh src, Mesh dst, Logger log) {
		if (src.getIndexArraysCount() > 1) {
			throw new IllegalArgumentException("doesn't support more than 1 index array")
		}
		if (src.getLod() > 1) {
			throw new IllegalArgumentException("doesn't support lod > 1 : "+ src.getLod())
		}

		dst.setMode(cnv(src.getPrimitive()));
		for(Datas.VertexArray va : src.getVertexArraysList()) {
			val type = cnv(va.getAttrib())
			dst.setBuffer(type, va.getFloats().getStep(), hack_cnv(va.getFloats()))
			log.debug("add {}", dst.getBuffer(type))
		}
		for(Datas.IndexArray va : src.getIndexArraysList()) {
			dst.setBuffer(VertexBuffer.Type.Index, va.getInts().getStep(), hack_cnv(va.getInts()))
		}
		// basic check
		val nbVertices = dst.getBuffer(VertexBuffer.Type.Position).getNumElements()
		for(IntMap.Entry<VertexBuffer> evb : dst.getBuffers()) {
			if (evb.getKey() != VertexBuffer.Type.Index.ordinal()) {
				if (nbVertices != evb.getValue().getNumElements()) {
					log.warn("size of vertex buffer {} is not equals to vertex buffer for position: {} != {}", VertexBuffer.Type.values().get(evb.getKey()), evb.getValue().getNumElements(), nbVertices)
				}
			}
		}
		//TODO optimize lazy create Tangent when needed (for normal map ?)
		if (dst.getBuffer(VertexBuffer.Type.Tangent) == null && dst.getBuffer(VertexBuffer.Type.TexCoord) != null) {
			TangentBinormalGenerator.generate(dst)
		}

		dst.updateCounts()
		dst.updateBound()
		dst
	}

	def Geometry cnv(Datas.Geometry src, Geometry dst, Logger log) {
		if (src.getMeshesCount() > 1) {
			throw new IllegalArgumentException("doesn't support more than 1 mesh")
		}
		dst.setName(if (src.hasName()) { src.getName() } else { src.getId()})
		dst.setMesh(cnv(src.getMeshes(0), new Mesh(), log))
		dst.updateModelBound()
		dst
	}

	//TODO optimize to create less intermediate node
	def merge(Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		mergeTObjects(src, root, components, log)
		mergeGeometries(src, root, components, log)
		mergeMaterials(src, components, log)
		mergeLights(src, root, components, log)
		mergeSkeletons(src, root, components, log)
		mergeCustomParams(src, components, log)
		mergeAnimations(src, components, log)
		// relations should be the last because it reuse data provide by other (put in components)
		mergeRelations(src, root, components, log)
	}

	def mergeAnimations(Datas.Data src, Map<String, Object> components, Logger log) {
		System.out.println("size anim : " + src.getExtension(AnimationsKf.animationsKf).size());
		for(AnimationsKf.AnimationKF e : src.getExtension(AnimationsKf.animationsKf)) {
			val id = e.getId()
			//TODO: merge with existing
			val child = makeAnimation(e, log)
			components.put(id, child)
			System.out.println("create Animation :" + id + " .. " + child)
		}
	}

	def Animation makeAnimation(AnimationKF e, Logger log) {
		val a =  new Animation(e.getName(), e.getDuration())
		for(AnimationsKf.Clip clip: e.getClipsList()) {
			if (clip.hasTransforms()) {
				a.addTrack(makeTrack(clip.getTransforms(), e.getDuration()))
			}
		}
		a
	}

	def Track makeTrack(TransformKF transforms, float duration) {
		val track = new CompositeTrack()
		val vkf = transforms.getTranslation()
		if (vkf.getXCount() > 0) {
			track.tracks.add(TrackFactory.translationX(cnv(vkf.getXList(), new FloatKeyPoints(), duration)))
		}
		if (vkf.getYCount() > 0) {
			track.tracks.add(TrackFactory.translationY(cnv(vkf.getYList(), new FloatKeyPoints(), duration)))
		}
		if (vkf.getZCount() > 0) {
			track.tracks.add(TrackFactory.translationZ(cnv(vkf.getZList(), new FloatKeyPoints(), duration)))
		}

		val vkf2 = transforms.getScale()
		if (vkf2.getXCount() > 0) {
			track.tracks.add(TrackFactory.scaleX(cnv(vkf2.getXList(), new FloatKeyPoints(), duration)))
		}
		if (vkf2.getYCount() > 0) {
			track.tracks.add(TrackFactory.scaleY(cnv(vkf2.getYList(), new FloatKeyPoints(), duration)))
		}
		if (vkf2.getZCount() > 0) {
			track.tracks.add(TrackFactory.scaleZ(cnv(vkf2.getZList(), new FloatKeyPoints(), duration)))
		}

		val vkf3 = transforms.getRotation();
		if (vkf3.getXCount() > 0) {
			track.tracks.add(TrackFactory.rotationX(cnv(vkf3.getXList(), new FloatKeyPoints(), duration)));
		}
		if (vkf3.getYCount() > 0) {
			track.tracks.add(TrackFactory.rotationY(cnv(vkf3.getYList(), new FloatKeyPoints(), duration)));
		}
		if (vkf3.getZCount() > 0) {
			track.tracks.add(TrackFactory.rotationZ(cnv(vkf3.getZList(), new FloatKeyPoints(), duration)));
		}
		if (vkf3.getWCount() > 0) {
			track.tracks.add(TrackFactory.rotationW(cnv(vkf3.getWList(), new FloatKeyPoints(), duration)));
		}
		track
	}

	def FloatKeyPoints cnv(List<KeyFrame> src, FloatKeyPoints dst, float duration) {
		val times = newFloatArrayOfSize(src.size())
		val values = newFloatArrayOfSize(src.size())
		for(var i = 0; i < times.length; i++) {
			val kf = src.get(i)
			times.set(i, kf.getDurationRatio() * duration)
			values.set(i, kf.getValue())
		}
		dst.setKeyPoints(times, values)
		dst.setEases(null, new Identity())
		dst
	}

	def void mergeSkeletons(Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		for(Datas.Skeleton e : src.getSkeletonsList()) {
			//TODO manage parent hierarchy
			val id = e.getId();
			//TODO: merge with existing
			val child = makeSkeleton(e, log);
			components.put(id, child);
			//Skeleton child = (Skeleton)components.get(id);
		}
	}

	def Skeleton makeSkeleton(Datas.Skeleton e, Logger log) {
		val bones = <Bone>newArrayOfSize(e.getBonesCount())
		val db = new HashMap<String, Bone>()
		for(var i = 0; i < bones.length; i++) {
			val src = e.getBones(i)
			val b = new Bone(src.getName())
			b.setBindTransforms(cnv(src.getTransform().getTranslation(), new Vector3f())
				, cnv(src.getTransform().getRotation(), new Quaternion())
				, cnv(src.getTransform().getScale(), new Vector3f())
			)
			db.put(src.getId(), b)
			bones.set(i, b)
		}
		for(Datas.Relation r : e.getBonesGraphList()) {
			val parent = db.get(r.getRef1())
			val child = db.get(r.getRef2())
			parent.addChild(child)
		}
		val sk = new Skeleton(bones)
		sk.setBindingPose()
		sk
	}

	def mergeCustomParams(Data src, Map<String, Object> components, Logger log) {
		for(CustomParams.CustomParamList srccp : src.getExtension(CustomParams.customParams)) {
			//TODO merge with existing
			components.put(srccp.getId(), srccp)
		}
	}

	def mergeLights(Data src, Node root, Map<String, Object> components, Logger log) {
		for(Datas.Light srcl : src.getLightsList()) {
			//TODO manage parent hierarchy
			val id = srcl.getId()
			var dst = components.get(id) as PgexLightControl
			if (dst == null) {
				dst = new PgexLightControl()
				components.put(id, dst)
				root.addControl(dst)
			}
			if (dst.light != null) {
				root.removeLight(dst.light)
			}
			dst.light = makeLight(srcl, log)
			root.addLight(dst.light)

			if (srcl.hasColor()) {
				dst.light.setColor(cnv(srcl.getColor(), new ColorRGBA()).mult(srcl.getIntensity()))
			}
			//TODO manage attenuation
			//TODO manage conversion of type
			switch(srcl.getKind()) {
			case spot: {
				val l = dst.light as SpotLight
				if (srcl.hasSpotAngle()) {
					val max = srcl.getSpotAngle().getMax()
					switch(srcl.getSpotAngle().getCurveCase()) {
						case CURVE_NOT_SET: {
							l.setSpotOuterAngle(max)
							l.setSpotInnerAngle(max)
						}
						case LINEAR: {
							l.setSpotOuterAngle(max * srcl.getSpotAngle().getLinear().getEnd())
							l.setSpotInnerAngle(max * srcl.getSpotAngle().getLinear().getBegin())
						}
						default: {
							l.setSpotOuterAngle(max)
							l.setSpotInnerAngle(max)
							log.warn("doesn't support curve like {} for spot_angle", srcl.getSpotAngle().getCurveCase())
						}
					}

				}
				if (srcl.hasRadialDistance()) {
					l.setSpotRange(srcl.getRadialDistance().getMax());
				}
			}
			case point: {
				val l = dst.light as PointLight
				if (srcl.hasRadialDistance()) {
					val max = srcl.getRadialDistance().getMax();
					switch(srcl.getRadialDistance().getCurveCase()) {
					case CURVE_NOT_SET: {
						l.setRadius(max);
					}
					case LINEAR: {
						l.setRadius(max * srcl.getSpotAngle().getLinear().getEnd());
					}
					case SMOOTH: {
						l.setRadius(max * srcl.getSpotAngle().getSmooth().getEnd());
					}
					default: {
						l.setRadius(max);
						log.warn("doesn't support curve like {} for spot_angle", srcl.getSpotAngle().getCurveCase());
					}
					}
				}
			}
			case ambient: {}
			case directional: {}
			}
		}
	}

	def Light makeLight(Datas.Light srcl, Logger log) {
		var l0 = null as Light
		switch(srcl.getKind()) {
			case ambient:
				l0 = new AmbientLight()
			case directional:
				l0 = new DirectionalLight()
			case spot: {
				val l = new SpotLight()
				l.setSpotRange(1000)
				l.setSpotInnerAngle(5f * FastMath.DEG_TO_RAD)
				l.setSpotOuterAngle(10f * FastMath.DEG_TO_RAD)
				l0 = l
			}
			case point:
				l0 = new PointLight()
		}
		l0.setColor(ColorRGBA.White.mult(2))
		l0.setName(if (srcl.hasName()) srcl.getName() else srcl.getId())
		l0
	}

	def mergeTObjects(Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		for(Datas.TObject n : src.getTobjectsList()) {
			val id = n.getId()
			var child = components.get(id) as Spatial
			if (child == null) {
				child = new Node("")
				root.attachChild(child)
				components.put(id, child)
			}
			child.setName(if (n.hasName()) n.getName() else n.getId())
			merge(n.getTransform(), child, log)
		}
	}

	def mergeGeometries(Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		for(Datas.Geometry g : src.getGeometriesList()) {
			//TODO manage parent hierarchy
			val id = g.getId()
			val o = components.get(id)
			var child = if (o instanceof Node)  toGeometry(o) else (o as Geometry)
			if (child == null) {
				child = new Geometry()
				child.setMaterial(defaultMaterial)
				root.attachChild(child)
				components.put(id, child)
			}
			child = cnv(g, child, log)
		}
	}

	def Geometry toGeometry(Node src) {
		val dst = new Geometry()
		dst.setName(src.getName())
		dst.setBatchHint(src.getBatchHint())
		dst.setCullHint(src.getLocalCullHint())
		dst.setLocalTransform(src.getLocalTransform())
		val ls = src.getLocalLightList()
		for(Light l: ls) {
			dst.addLight(l);
		}
		ls.clear();
		for(String k : src.getUserDataKeys()) {
			dst.setUserData(k, src.getUserData(k));
		}
		dst
	}
	def mergeMaterials(Datas.Data src, Map<String, Object> components, Logger log) {
		for(Datas.Material m : src.getMaterialsList()) {
			//TODO update / remove previous material
			val id = m.getId()
			//val mat = components.get(id) as Material
			//if (mat == null) {
				val mat = newMaterial(m, log)
				components.put(id, mat)
			//}
			mat.setName(if (m.hasName()) m.getName() else m.getId())
			mergeToMaterial(m, mat, log)
		}
	}

	//TODO use dispatch or pattern matching of Xtend
	def void mergeRelations(Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		for(Datas.Relation r : src.getRelationsList()) {
			val op1 = components.get(r.getRef1());
			val op2 = components.get(r.getRef2());
			if (op1 == null) {
				log.warn("can't link op1 not found : {}", r.getRef1());
			}
			if (op2 == null) {
				log.warn("can't link op2 not found : {}", r.getRef2());
			}
			if (op1 != null && op2 != null) {
				var done = false;
				if (op1 instanceof Animation) {
					if (op2 instanceof Spatial) { // Geometry, Node
						val s = op2 as Spatial
						var c = s.getControl(typeof(AnimControl))
						if (c == null) {
							c = new AnimControl()
							s.addControl(c)
						}
						System.out.println("add Animation :" + op1)
						c.addAnim(op1 as Animation)
						done = true;
					}
				} else if (op1 instanceof CustomParamList) { // <--> pgex_ext.Customparams.CustomParams
					val cp1 = op1 as CustomParamList
					if (op2 instanceof Spatial) { // Geometry, Node
						for(CustomParam p : cp1.getParamsList()) {
							mergeToUserData(p, op2 as Spatial, log);
						}
						done = true;
					}
				}else if (op1 instanceof Geometry) { // <--> pgex.Datas.Geometry
					val g1 = op1 as Geometry
					if (op2 instanceof PgexLightControl) {
						val l2 = op2 as PgexLightControl
						l2.getSpatial().removeControl(l2);
						g1.addControl(l2);
						// TODO raise an alert, strange to link LightNode and Geometry
						done = true;
					} else if (op2 instanceof Material) {
						g1.setMaterial(op2 as Material);
						done = true;
					} else if (op2 instanceof Node) {
						(op2 as Node).attachChild(g1);
						done = true;
					} else if (op2 instanceof Skeleton) {
						link(g1, op2 as Skeleton);
						done = true;
					}
				} else if (op1 instanceof PgexLightControl) { // <--> pgex.Datas.Light
					val l1 = op1 as PgexLightControl
					if (op2 instanceof Node) {
						l1.getSpatial().removeControl(l1);
						(op2 as Node).addControl(l1)
						done = true;
					}
				} else if (op1 instanceof Material) { // <--> pgex.Datas.Material
					val m1 = op1 as Material;
					if (op2 instanceof Node) {
						(op2 as Node).setMaterial(m1)
						done = true
					}
				} else if (op1 instanceof Skeleton) { // <--> pgex.Datas.Skeleton
					if (op2 instanceof Node) {
						link(op2 as Node, op1 as Skeleton)
						done = true;
					}
					done = true;
				} else if (op1 instanceof Node) { // <--> pgex.Datas.TObject
					if (op2 instanceof Node) {
						(op1 as Node).attachChild(op2 as Node);
						done = true;
					}
				}
				if (!done) {
					log.warn("doesn't know how to make relation {}({}) -- {}({})\n", r.getRef1(), op1.getClass(), r.getRef2(), op2.getClass());
				}
			}
		}
	}

	// see http://hub.jmonkeyengine.org/t/skeletoncontrol-or-animcontrol-to-host-skeleton/31478/4
	def link(Spatial v, Skeleton sk) {
		v.removeControl(typeof(SkeletonControl))
		//update AnimControl if related to skeleton
		val ac = v.getControl(typeof(AnimControl))
		if (ac != null && ac.getSkeleton() != null) {
			v.removeControl(ac)
			v.addControl(new AnimControl(sk))
		}
		// SkeletonControl should be after AnimControl in the list of Controls
		v.addControl(new SkeletonControl(sk))
	}

	def Spatial mergeToUserData(CustomParam p, Spatial dst, Logger log) {
		val name = p.getName()
		switch(p.getValueCase()) {
		case VALUE_NOT_SET:
			dst.setUserData(name, null)
		case VBOOL:
			dst.setUserData(name, p.vbool)
		case VCOLOR:
			dst.setUserData(name, cnv(p.vcolor, new ColorRGBA()))
		case VFLOAT:
			dst.setUserData(name, p.vfloat)
		case VINT:
			dst.setUserData(name, p.vint)
		case VMAT4:
			dst.setUserData(name, cnv(p.vmat4, new Matrix4f()))
		case VQUAT:
			dst.setUserData(name, cnv(p.vquat, new Vector4f()))
		case VSTRING:
			dst.setUserData(name, p.vstring)
		case VTEXTURE:
			dst.setUserData(name, getValue(p.vtexture, log))
		case VVEC2:
			dst.setUserData(name, cnv(p.vvec2, new Vector2f()))
		case VVEC3:
			dst.setUserData(name, cnv(p.vvec3, new Vector3f()))
		case VVEC4:
			dst.setUserData(name, cnv(p.vvec4, new Vector4f()))
		default:
			log.warn("Material doesn't support parameter : {} of type {}", name, p.getValueCase().name())
		}
		return dst;
	}

	def Image.Format getValue(Datas.Texture2DInline.Format f, Logger log) {
		switch(f){
			//case bgra8: return Image.Format.BGR8;
			case rgb8: Image.Format.RGB8
			case rgba8: Image.Format.RGBA8
			default: throw new UnsupportedOperationException("image format :" + f)
		}
	}

	def Texture getValue(Datas.Texture t, Logger log) {
		switch(t.getDataCase()){
			case DATA_NOT_SET: null
			case RPATH:
				assetManager.loadTexture(t.getRpath())
			case TEX2D: {
				val t2di = t.getTex2D()
				val img = new Image(getValue(t2di.getFormat(), log), t2di.getWidth(), t2di.getHeight(), t2di.getData().asReadOnlyByteBuffer());
				new Texture2D(img)
			}
			default:
				throw new IllegalArgumentException("doesn't support more than texture format:" + t.getDataCase())
		}
	}

	def Material newMaterial(Datas.Material m, Logger log) {
		val lightFamily = !m.getShadeless()
		val def = if (lightFamily) "Common/MatDefs/Light/Lighting.j3md" else "Common/MatDefs/Misc/Unshaded.j3md"
		val mat = new Material(assetManager, def)
		if (lightFamily) {
			mat.setBoolean("UseMaterialColors", true)
			mat.setBoolean("UseVertexColor", true)
		}
		mat
	}

	def Material mergeToMaterial(Datas.Material src, Material dst, Logger log) {
		val md = dst.getMaterialDef()
		setColor(src.hasColor(), src.getColor(), dst, #["Color", "Diffuse"], md, log)
		setTexture2D(src.hasColorMap(), src.getColorMap(), dst, #["ColorMap", "DiffuseMap"], md, log)
		//setTexture2D(src.hasNormalMap(), src.getNormalMap(), dst, new String[]{"ColorMap", "DiffuseMap"], md, log)
		setFloat(src.hasOpacity(), src.getOpacity(), dst, #["Alpha", "Opacity"], md, log)
		setTexture2D(src.hasOpacityMap(), src.getOpacityMap(), dst, #["AlphaMap", "OpacityMap"], md, log)
		setTexture2D(src.hasNormalMap(), src.getNormalMap(), dst, #["NormalMap"], md, log)
		setFloat(src.hasRoughness(), src.getRoughness(), dst, #["Roughness"], md, log)
		setTexture2D(src.hasRoughnessMap(), src.getRoughnessMap(), dst, #["RoughnessMap"], md, log)
		setFloat(src.hasMetalness(), src.getMetalness(), dst, #["Metalness"], md, log)
		setTexture2D(src.hasMetalnessMap(), src.getMetalnessMap(), dst, #["MetalnessMap"], md, log)
		setColor(src.hasSpecular(), src.getSpecular(), dst, #["Specular"], md, log)
		setTexture2D(src.hasSpecularMap(), src.getSpecularMap(), dst, #["SpecularMap"], md, log)
		setFloat(src.hasSpecularPower(), src.getSpecularPower(), dst, #["SpecularPower", "Shininess"], md, log)
		setTexture2D(src.hasSpecularPowerMap(), src.getSpecularPowerMap(), dst, #["SpecularPowerMap", "ShininessMap"], md, log)
		setColor(src.hasEmission(), src.getEmission(), dst, #["Emission", "GlowColor"], md, log)
		setTexture2D(src.hasEmissionMap(), src.getEmissionMap(), dst, #["EmissionMap", "GlowMap"], md, log)
		dst
	}

	def setColor(boolean has, Datas.Color src, Material dst, String[] names, MaterialDef scope, Logger log){
		if (has) {
			val name = findMaterialParamName(names, VarType.Vector4, scope, log)
			if (name != null) {
				dst.setColor(name, cnv(src, new ColorRGBA()))
			} else {
				log.warn("can't find a matching name for : [{}] ({})", String.join(",", names), VarType.Vector4)
			}
		}
	}

	def setTexture2D(boolean has, Datas.Texture src, Material dst, String[] names, MaterialDef scope, Logger log){
		if (has) {
			val name = findMaterialParamName(names, VarType.Texture2D, scope, log)
			if (name != null) {
				dst.setTexture(name, getValue(src, log))
			} else {
				log.warn("can't find a matching name for : [{}] ({})", String.join(",", names), VarType.Texture2D)
			}
		}
	}

	def setFloat(boolean has, float src, Material dst, String[] names, MaterialDef scope, Logger log){
		if (has) {
			val name = findMaterialParamName(names, VarType.Float, scope, log)
			if (name != null) {
				dst.setFloat(name, src)
			} else {
				log.warn("can't find a matching name for : [{}] ({})", String.join(",", names), VarType.Float)
			}
		}
	}

	def String findMaterialParamName(String[] names, VarType type, MaterialDef scope, Logger log) {
		for(String name2 : names){
			for(MatParam mp : scope.getMaterialParams()) {
				if (mp.getName().equalsIgnoreCase(name2) && mp.getVarType() == type) {
					return mp.getName()
				}
			}
		}
		null
	}

	def void merge(Datas.Transform src, Spatial dst, Logger log) {
		dst.setLocalRotation(cnv(src.getRotation(), dst.getLocalRotation()))
		dst.setLocalTranslation(cnv(src.getTranslation(), dst.getLocalTranslation()))
		dst.setLocalScale(cnv(src.getScale(), dst.getLocalScale()))
	}
}

