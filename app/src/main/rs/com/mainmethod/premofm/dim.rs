#pragma version(1)
#pragma rs java_package_name(com.mainmethod.premofm)
#pragma rs_fp_relaxed

float dimmingValue = .4f;

uchar4 __attribute__((kernel)) dim(uchar4 in)
{
    uchar4 out = in;
    out.r = in.r * dimmingValue;
    out.g = in.g * dimmingValue;
    out.b = in.b * dimmingValue;
    return out;
}