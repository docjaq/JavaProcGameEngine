#version 330

//From vertex shader
in vec4 diffuseColor;
in vec3 vertexNormal;
in vec3 cameraSpacePosition;

out vec4 outputColor;

//From CPU
uniform vec4 lightIntensity;
uniform vec4 ambientIntensity;
uniform vec3 cameraSpaceLightPos;
uniform float lightAttenuation;
const vec4 specularColor = vec4(0.25, 0.25, 0.65, 1.0);
uniform float shininessFactor;

uniform samplerCube cubeMap;

float calcAttenuation(in vec3 cameraSpacePosition, out vec3 lightDirection)
{
	vec3 lightDifference = cameraSpaceLightPos - cameraSpacePosition;
	float lightDistanceSqr = dot(lightDifference, lightDifference);
	lightDirection = lightDifference * inversesqrt(lightDistanceSqr);
	
	return 1.0 / (1.0 + lightAttenuation * sqrt(lightDistanceSqr));
}

mat4 rotationMatrix(vec3 axis, float angle)
{
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;
    
    return mat4(oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0.0,
                oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0.0,
                oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0.0,
                0.0,                                0.0,                                0.0,                                1.0);
}

vec4 calcCubeMapValue(vec3 viewDirection, vec3 normal){
    float rIndex = 1.0/1.333333;
    vec3 r = refract(viewDirection, normal, rIndex);
    //Currently have to do this stupid rotation because we're looking down the z axis, and the
    //sky box is not set up that way. Alternatively, could modify the cube map images
    //(i.e. replace *and* rotate), but... eugh.
    r = (vec4(r, 1)*rotationMatrix(vec3(1, 0, 0), -1.57079633)).xyz;
    return vec4(texture(cubeMap, r));
}

void main()
{
	vec3 lightDir = vec3(0.0);
	float atten = calcAttenuation(cameraSpacePosition, lightDir);
	vec4 attenIntensity = atten * lightIntensity;
	
	vec3 surfaceNormal = normalize(vec3(vertexNormal.x, vertexNormal.y, vertexNormal.z));
	
    
    
	float cosAngIncidence = dot(surfaceNormal, lightDir);
	cosAngIncidence = clamp(cosAngIncidence, 0.0, 1.0);
	
	vec3 viewDirection = normalize(-cameraSpacePosition);
	
	vec3 halfAngle = normalize(lightDir + viewDirection);
	float angleNormalHalf = acos(dot(halfAngle, surfaceNormal));
	float exponent = angleNormalHalf / shininessFactor;
	exponent = -(exponent * exponent);
	float gaussianTerm = exp(exponent);
	
	gaussianTerm = cosAngIncidence != 0.0 ? gaussianTerm : 0.0;
	   
	outputColor = diffuseColor  * attenIntensity * cosAngIncidence +
    specularColor * attenIntensity * gaussianTerm +
    diffuseColor  * ambientIntensity;
    
    //Compute cubemap color
    vec4 cubeMapColor = calcCubeMapValue(viewDirection, surfaceNormal);
    //Blend cubemap color and gaussian color
    outputColor = outputColor * cubeMapColor;
    outputColor.a = 0.550;
}