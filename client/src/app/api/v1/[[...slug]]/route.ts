import { NextResponse } from 'next/server';

async function getPathSegments(params: Promise<{ slug?: string[] }>) {
  const resolvedParams = await params;
  return resolvedParams.slug || [];
}

export async function GET(
  req: Request,
  { params }: { params: Promise<{ slug?: string[] }> }
) {
  const slug = await getPathSegments(params);
  const path = slug.join('/');

  if (path === 'users/me') {
    return NextResponse.json({
      publicId: 'user-1',
      email: 'admin@vinqa.com',
      displayName: 'Administrator',
      role: 'ADMIN',
      status: 'ACTIVE',
      lastLoginAt: new Date().toISOString(),
    });
  }

  if (path === 'projects') {
    return NextResponse.json({
      items: [
        {
          publicId: 'proj-1',
          name: 'SmartCare Chatbot QA',
          description: 'SmartCare Chatbot QA evaluation project.',
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          archivedAt: null,
        },
        {
          publicId: 'proj-2',
          name: 'VinMed Health Assistant',
          description: 'VinMed Health Assistant evaluation project.',
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          archivedAt: null,
        },
      ],
      page: 0,
      size: 10,
      totalItems: 2,
      totalPages: 1,
    });
  }

  if (path === 'rubrics') {
    return NextResponse.json({
      items: [
        {
          publicId: 'rubric-1',
          name: 'General Safety Rubric',
          description: 'Checks safety policies and answers alignment.',
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
      ],
      page: 0,
      size: 10,
      totalItems: 1,
      totalPages: 1,
    });
  }

  return NextResponse.json(
    { code: 'NOT_FOUND', message: `Mock GET not found for path: ${path}` },
    { status: 404 }
  );
}

export async function POST(
  req: Request,
  { params }: { params: Promise<{ slug?: string[] }> }
) {
  const slug = await getPathSegments(params);
  const path = slug.join('/');

  if (path === 'auth/login') {
    return NextResponse.json({
      accessToken: 'mock-access-token-jwt',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: {
        publicId: 'user-1',
        email: 'admin@vinqa.com',
        displayName: 'Administrator',
        role: 'ADMIN',
        status: 'ACTIVE',
        lastLoginAt: new Date().toISOString(),
      },
    });
  }

  if (path === 'auth/refresh-token') {
    return NextResponse.json({
      accessToken: 'mock-refreshed-access-token-jwt',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
    });
  }

  if (path === 'auth/logout') {
    return new Response(null, { status: 204 });
  }

  return NextResponse.json(
    { code: 'NOT_FOUND', message: `Mock POST not found for path: ${path}` },
    { status: 404 }
  );
}

export async function PUT(
  req: Request,
  { params }: { params: Promise<{ slug?: string[] }> }
) {
  const slug = await getPathSegments(params);
  const path = slug.join('/');
  return NextResponse.json({ message: `Mock PUT for path: ${path}` });
}

export async function PATCH(
  req: Request,
  { params }: { params: Promise<{ slug?: string[] }> }
) {
  const slug = await getPathSegments(params);
  const path = slug.join('/');
  return NextResponse.json({ message: `Mock PATCH for path: ${path}` });
}

export async function DELETE(
  req: Request,
  { params }: { params: Promise<{ slug?: string[] }> }
) {
  const slug = await getPathSegments(params);
  const path = slug.join('/');
  return NextResponse.json({ message: `Mock DELETE for path: ${path}` });
}
